package com.dating.owoke.dating.couple.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.couple.domain.Couple;
import com.dating.owoke.dating.couple.domain.CoupleInvitation;
import com.dating.owoke.dating.couple.domain.CoupleMember;
import com.dating.owoke.dating.couple.domain.CoupleMemberRole;
import com.dating.owoke.dating.couple.domain.CoupleStatus;
import com.dating.owoke.dating.couple.domain.InvitationStatus;
import com.dating.owoke.dating.couple.repository.CoupleInvitationRepository;
import com.dating.owoke.dating.couple.repository.CoupleMemberRepository;
import com.dating.owoke.dating.couple.repository.CoupleRepository;
import com.dating.owoke.dating.shared.exception.BusinessConflictException;
import com.dating.owoke.dating.shared.exception.InvitationUnavailableException;
import com.dating.owoke.dating.shared.exception.ResourceNotFoundException;
import com.dating.owoke.dating.shared.messaging.event.CoupleActivatedV1;
import com.dating.owoke.dating.shared.messaging.service.OutboxService;

@Service
public class CoupleInvitationService {

    private static final Duration INVITATION_TTL = Duration.ofDays(7);
    private static final String DATING_EVENTS_TOPIC = "dating.events.v1";

    private final CoupleRepository coupleRepository;
    private final CoupleMemberRepository memberRepository;
    private final CoupleInvitationRepository invitationRepository;
    private final InvitationTokenService tokenService;
    private final OutboxService outboxService;
    private final Clock clock;
    private final String webAppUrl;

    public CoupleInvitationService(
            CoupleRepository coupleRepository,
            CoupleMemberRepository memberRepository,
            CoupleInvitationRepository invitationRepository,
            InvitationTokenService tokenService,
            OutboxService outboxService,
            Clock clock,
            @Value("${owoke.web-app-url}") String webAppUrl
    ) {
        this.coupleRepository = coupleRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.tokenService = tokenService;
        this.outboxService = outboxService;
        this.clock = clock;
        this.webAppUrl = webAppUrl;
    }

    @Transactional
    public InvitationCreation create(UUID inviterId) {
        Instant now = clock.instant();
        CoupleMember current = memberRepository.findActiveByUserIdForUpdate(inviterId).orElse(null);
        Couple couple;
        if (current == null) {
            couple = coupleRepository.save(Couple.pending(inviterId, now));
            memberRepository.save(new CoupleMember(couple.getId(), inviterId, CoupleMemberRole.OWNER, now));
        } else {
            couple = coupleRepository.findByIdForUpdate(current.getCoupleId())
                    .orElseThrow(() -> new IllegalStateException("Membership references a missing couple"));
            if (couple.getStatus() != CoupleStatus.PENDING || current.getRole() != CoupleMemberRole.OWNER) {
                throw new BusinessConflictException("User already belongs to a couple");
            }
            invitationRepository.findByCoupleIdAndStatus(couple.getId(), InvitationStatus.PENDING)
                    .forEach(invitation -> invitation.revoke(now));
            invitationRepository.flush();
        }

        String rawToken = tokenService.generate();
        Instant expiresAt = now.plus(INVITATION_TTL);
        CoupleInvitation invitation = invitationRepository.save(new CoupleInvitation(
                couple.getId(), inviterId, tokenService.hash(rawToken), now, expiresAt));
        return new InvitationCreation(
                invitation.getId(),
                webAppUrl + "/invite/" + rawToken,
                expiresAt);
    }

    @Transactional(readOnly = true)
    public InvitationPreview preview(String rawToken) {
        CoupleInvitation invitation = invitationRepository.findByTokenHash(tokenService.hash(rawToken))
                .orElseThrow(() -> new InvitationUnavailableException("Invitation is invalid or unavailable"));
        if (!invitation.isUsableAt(clock.instant())) {
            throw new InvitationUnavailableException("Invitation is expired or unavailable");
        }
        return new InvitationPreview(invitation.getId(), invitation.getExpiresAt());
    }

    @Transactional
    public CoupleDetails accept(String rawToken, UUID partnerId) {
        Instant now = clock.instant();
        CoupleInvitation invitation = invitationRepository.findByTokenHashForUpdate(tokenService.hash(rawToken))
                .orElseThrow(() -> new InvitationUnavailableException("Invitation is invalid or unavailable"));
        if (!invitation.isUsableAt(now)) {
            throw new InvitationUnavailableException("Invitation is expired or unavailable");
        }
        if (invitation.getInviterId().equals(partnerId)) {
            throw new BusinessConflictException("An invitation cannot be accepted by its creator");
        }
        if (memberRepository.findActiveByUserIdForUpdate(partnerId).isPresent()) {
            throw new BusinessConflictException("User already belongs to a couple");
        }

        Couple couple = coupleRepository.findByIdForUpdate(invitation.getCoupleId())
                .orElseThrow(() -> new IllegalStateException("Invitation references a missing couple"));
        if (couple.getStatus() != CoupleStatus.PENDING) {
            throw new InvitationUnavailableException("Invitation is unavailable");
        }
        List<CoupleMember> existingMembers =
                memberRepository.findByCoupleIdAndLeftAtIsNullOrderByJoinedAt(couple.getId());
        if (existingMembers.size() != 1 || existingMembers.getFirst().getRole() != CoupleMemberRole.OWNER) {
            throw new IllegalStateException("Pending couple has invalid membership state");
        }

        CoupleMember partner = memberRepository.saveAndFlush(new CoupleMember(
                couple.getId(), partnerId, CoupleMemberRole.PARTNER, now));
        invitation.accept(now);
        couple.activate(now);
        UUID ownerId = existingMembers.getFirst().getUserId();
        outboxService.enqueue(
                DATING_EVENTS_TOPIC,
                couple.getId().toString(),
                "CoupleActivatedV1",
                new CoupleActivatedV1(couple.getId(), ownerId, partnerId, now));
        return CoupleService.details(couple, List.of(existingMembers.getFirst(), partner));
    }

    @Transactional
    public void revoke(UUID invitationId, UUID userId) {
        CoupleInvitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation was not found"));
        if (!invitation.getInviterId().equals(userId)) {
            throw new ResourceNotFoundException("Invitation was not found");
        }
        invitation.revoke(clock.instant());
    }
}
