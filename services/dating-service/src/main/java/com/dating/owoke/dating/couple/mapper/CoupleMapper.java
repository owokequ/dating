package com.dating.owoke.dating.couple.mapper;

import org.springframework.stereotype.Component;

import com.dating.owoke.dating.couple.dto.CoupleMemberResponse;
import com.dating.owoke.dating.couple.dto.CoupleResponse;
import com.dating.owoke.dating.couple.dto.InvitationCreationResponse;
import com.dating.owoke.dating.couple.dto.InvitationPreviewResponse;
import com.dating.owoke.dating.couple.service.CoupleDetails;
import com.dating.owoke.dating.couple.service.InvitationCreation;
import com.dating.owoke.dating.couple.service.InvitationPreview;
import com.dating.owoke.dating.userprojection.repository.UserProfileProjectionRepository;

@Component
public class CoupleMapper {
    private final UserProfileProjectionRepository profiles;
    public CoupleMapper(UserProfileProjectionRepository profiles) { this.profiles = profiles; }

    public CoupleResponse toResponse(CoupleDetails details) {
        return new CoupleResponse(
                details.id(),
                details.status(),
                details.members().stream()
                        .map(member -> new CoupleMemberResponse(
                                member.userId(), profiles.findById(member.userId()).map(value -> value.getDisplayName()).orElse(null), member.role(), member.joinedAt()))
                        .toList(),
                details.createdAt(),
                details.activatedAt(),
                details.version());
    }

    public InvitationCreationResponse toResponse(InvitationCreation creation) {
        return new InvitationCreationResponse(
                creation.invitationId(), creation.inviteUrl(), creation.expiresAt());
    }

    public InvitationPreviewResponse toResponse(InvitationPreview preview) {
        return new InvitationPreviewResponse(preview.invitationId(), preview.expiresAt());
    }
}
