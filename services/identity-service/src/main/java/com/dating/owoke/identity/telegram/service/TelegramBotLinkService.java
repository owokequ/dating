package com.dating.owoke.identity.telegram.service;

import java.time.Clock;

import org.springframework.stereotype.Service;

import com.dating.owoke.identity.account.domain.UserAccount;
import com.dating.owoke.identity.account.repository.UserAccountRepository;
import com.dating.owoke.identity.authentication.domain.AccountToken;
import com.dating.owoke.identity.authentication.domain.AccountTokenType;
import com.dating.owoke.identity.authentication.repository.AccountTokenRepository;
import com.dating.owoke.identity.authentication.service.SecureTokenGenerator;
import com.dating.owoke.identity.shared.messaging.event.UserTelegramLinkedV1;
import com.dating.owoke.identity.shared.messaging.service.OutboxService;
import com.dating.owoke.identity.telegram.domain.ExternalIdentity;
import com.dating.owoke.identity.telegram.domain.ExternalProvider;
import com.dating.owoke.identity.telegram.repository.ExternalIdentityRepository;

@Service
public class TelegramBotLinkService {

    private static final String IDENTITY_EVENTS_TOPIC = "identity.events.v1";

    private final AccountTokenRepository tokenRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final UserAccountRepository userRepository;
    private final ExternalIdentityRepository identityRepository;
    private final OutboxService outboxService;
    private final Clock clock;

    public TelegramBotLinkService(
            AccountTokenRepository tokenRepository,
            SecureTokenGenerator tokenGenerator,
            UserAccountRepository userRepository,
            ExternalIdentityRepository identityRepository,
            OutboxService outboxService,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    public void link(TelegramLinkRequestedV1 command) {
        AccountToken token = tokenRepository.findByTokenHashAndType(
                        tokenGenerator.hash(command.linkToken()), AccountTokenType.TELEGRAM_LINK)
                .orElseThrow(() -> new IllegalArgumentException("Telegram link token is invalid"));
        token.consume(clock.instant());

        UserAccount account = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("Telegram link token has no user"));
        ExternalIdentity byTelegram = identityRepository.findByTelegramUserId(command.telegramUserId()).orElse(null);
        if (byTelegram != null && !byTelegram.getUserId().equals(account.getId())) {
            throw new IllegalStateException("Telegram account is already linked to another user");
        }

        ExternalIdentity identity = identityRepository
                .findByUserIdAndProvider(account.getId(), ExternalProvider.TELEGRAM)
                .orElse(null);
        if (identity == null) {
            identity = identityRepository.save(new ExternalIdentity(
                    account.getId(),
                    "bot:" + command.telegramUserId(),
                    command.telegramUserId(),
                    command.username(),
                    true,
                    clock.instant()));
        } else {
            if (!identity.getTelegramUserId().equals(command.telegramUserId())) {
                throw new IllegalStateException("Owoke user already has another Telegram account");
            }
            identity.recordLogin(command.username(), true, clock.instant());
        }

        outboxService.enqueue(
                IDENTITY_EVENTS_TOPIC,
                account.getId().toString(),
                "UserTelegramLinkedV1",
                new UserTelegramLinkedV1(
                        account.getId(),
                        identity.getTelegramUserId(),
                        identity.getUsername(),
                        true));
    }
}
