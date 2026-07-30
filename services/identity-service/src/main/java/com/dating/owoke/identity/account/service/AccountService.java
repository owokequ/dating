package com.dating.owoke.identity.account.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.identity.account.domain.UserAccount;
import com.dating.owoke.identity.account.exception.AccountNotFoundException;
import com.dating.owoke.identity.account.repository.UserAccountRepository;
import com.dating.owoke.identity.shared.messaging.event.UserProfileUpdatedV1;
import com.dating.owoke.identity.shared.messaging.service.OutboxService;
import com.dating.owoke.identity.telegram.domain.ExternalProvider;
import com.dating.owoke.identity.telegram.repository.ExternalIdentityRepository;

@Service
public class AccountService {

    private static final String IDENTITY_EVENTS_TOPIC = "identity.events.v1";

    private final UserAccountRepository userRepository;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final OutboxService outboxService;
    private final Clock clock;

    public AccountService(
            UserAccountRepository userRepository,
            ExternalIdentityRepository externalIdentityRepository,
            OutboxService outboxService,
            Clock clock) {
        this.userRepository = userRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountProfile get(UUID userId) {
        UserAccount account = userRepository.findById(userId).orElseThrow(AccountNotFoundException::new);
        boolean telegramLinked = externalIdentityRepository
                .findByUserIdAndProvider(userId, ExternalProvider.TELEGRAM)
                .isPresent();
        return toProfile(account, telegramLinked);
    }

    @Transactional
    public AccountProfile updateDisplayName(UUID userId, String displayName) {
        UserAccount account = userRepository.findById(userId).orElseThrow(AccountNotFoundException::new);
        account.changeDisplayName(displayName, clock.instant());
        outboxService.enqueue(
                IDENTITY_EVENTS_TOPIC,
                userId.toString(),
                "UserProfileUpdatedV1",
                new UserProfileUpdatedV1(userId, account.getDisplayName()));
        boolean telegramLinked = externalIdentityRepository
                .findByUserIdAndProvider(userId, ExternalProvider.TELEGRAM)
                .isPresent();
        return toProfile(account, telegramLinked);
    }

    private static AccountProfile toProfile(UserAccount account, boolean telegramLinked) {
        return new AccountProfile(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getStatus(),
                telegramLinked);
    }
}
