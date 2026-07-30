package com.dating.owoke.dating.couple.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.couple.domain.Couple;
import com.dating.owoke.dating.couple.domain.CoupleMember;
import com.dating.owoke.dating.couple.domain.InvitationStatus;
import com.dating.owoke.dating.couple.repository.CoupleInvitationRepository;
import com.dating.owoke.dating.couple.repository.CoupleMemberRepository;
import com.dating.owoke.dating.couple.repository.CoupleRepository;
import com.dating.owoke.dating.shared.exception.ResourceNotFoundException;
import com.dating.owoke.dating.shared.messaging.event.CoupleClosedV1;
import com.dating.owoke.dating.shared.messaging.service.OutboxService;

@Service
public class CoupleService {

    private static final String DATING_EVENTS_TOPIC = "dating.events.v1";

    private final CoupleRepository coupleRepository;
    private final CoupleMemberRepository memberRepository;
    private final CoupleInvitationRepository invitationRepository;
    private final OutboxService outboxService;
    private final Clock clock;

    public CoupleService(
            CoupleRepository coupleRepository,
            CoupleMemberRepository memberRepository,
            CoupleInvitationRepository invitationRepository,
            OutboxService outboxService,
            Clock clock
    ) {
        this.coupleRepository = coupleRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CoupleDetails getCurrent(UUID userId) {
        CoupleMember membership = memberRepository.findByUserIdAndLeftAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User does not have a current couple"));
        Couple couple = coupleRepository.findById(membership.getCoupleId())
                .orElseThrow(() -> new IllegalStateException("Membership references a missing couple"));
        return details(couple, memberRepository.findByCoupleIdAndLeftAtIsNullOrderByJoinedAt(couple.getId()));
    }

    @Transactional
    public void closeCurrent(UUID userId) {
        CoupleMember membership = memberRepository.findActiveByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User does not have a current couple"));
        Couple couple = coupleRepository.findByIdForUpdate(membership.getCoupleId())
                .orElseThrow(() -> new IllegalStateException("Membership references a missing couple"));
        List<CoupleMember> members = memberRepository.findByCoupleIdAndLeftAtIsNullOrderByJoinedAt(couple.getId());
        Instant now = clock.instant();
        couple.close(now);
        members.forEach(member -> member.leave(now));
        invitationRepository.findByCoupleIdAndStatus(couple.getId(), InvitationStatus.PENDING)
                .forEach(invitation -> invitation.revoke(now));
        outboxService.enqueue(
                DATING_EVENTS_TOPIC,
                couple.getId().toString(),
                "CoupleClosedV1",
                new CoupleClosedV1(
                        couple.getId(),
                        members.stream().map(CoupleMember::getUserId).toList(),
                        userId,
                        now));
    }

    static CoupleDetails details(Couple couple, List<CoupleMember> members) {
        return new CoupleDetails(
                couple.getId(),
                couple.getStatus(),
                members.stream()
                        .map(member -> new CoupleDetails.MemberDetails(
                                member.getUserId(), member.getRole(), member.getJoinedAt()))
                        .toList(),
                couple.getCreatedAt(),
                couple.getActivatedAt(),
                couple.getVersion());
    }
}
