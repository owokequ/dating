package com.dating.owoke.notification.telegram.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.telegram.domain.TelegramDecisionRequest;
import com.dating.owoke.notification.telegram.repository.TelegramDecisionRequestRepository;

@Service
public class TelegramDecisionService {
    private final TelegramDecisionRequestRepository repository;
    private final Clock clock;

    public TelegramDecisionService(TelegramDecisionRequestRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void remember(
            UUID requestId, long updateId, UUID proposalId, UUID actorId,
            long chatId, long messageId, String actionUrl) {
        repository.save(new TelegramDecisionRequest(
                requestId, updateId, proposalId, actorId, chatId, messageId, actionUrl, clock.instant()));
    }

    @Transactional
    public boolean result(UUID requestId, UUID actorId, String caption) {
        TelegramDecisionRequest request = repository.findById(requestId).orElse(null);
        if (request == null || !request.getActorId().equals(actorId)) return false;
        request.result(caption, clock.instant());
        return true;
    }

    @Transactional
    public List<TelegramDecisionTask> claim() {
        return repository.lockReady(clock.instant()).stream().map(request -> {
            request.processing(clock.instant());
            return new TelegramDecisionTask(
                    request.getRequestId(), request.getProposalId(), request.getActorId(),
                    request.getChatId(), request.getMessageId(),
                    request.getResultCaption(), request.getActionUrl());
        }).toList();
    }

    @Transactional
    public void complete(UUID requestId) {
        repository.findById(requestId).orElseThrow().completed(clock.instant());
    }

    @Transactional
    public void fail(UUID requestId) {
        repository.findById(requestId).orElseThrow().failed(clock.instant());
    }
}
