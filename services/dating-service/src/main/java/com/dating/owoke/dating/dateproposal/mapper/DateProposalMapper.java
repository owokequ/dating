package com.dating.owoke.dating.dateproposal.mapper;

import org.springframework.stereotype.Component;

import com.dating.owoke.dating.dateproposal.dto.DateProposalResponse;
import com.dating.owoke.dating.dateproposal.service.DateProposalDetails;

@Component
public class DateProposalMapper {

    public DateProposalResponse toResponse(DateProposalDetails details) {
        return new DateProposalResponse(
                details.id(), details.coupleId(), details.proposerId(), details.responderId(),
                details.scheduledAt(), details.timezone(), details.selectionType(), details.placeId(), details.placeName(),
                details.placeAddress(), details.placeCoverMediaId(), details.eventId(), details.eventOccurrenceId(),
                details.eventTitle(), details.eventSourceUrl(), details.eventPrice(), details.description(), details.status(), details.createdAt(),
                details.decidedAt(), details.cancelledAt(), details.version());
    }
}
