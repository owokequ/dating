package com.dating.owoke.identity.telegram.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.identity.account.domain.UserAccount;
import com.dating.owoke.identity.account.repository.UserAccountRepository;
import com.dating.owoke.identity.authentication.exception.AuthenticationRejectedException;
import com.dating.owoke.identity.authentication.service.AuthenticationService;
import com.dating.owoke.identity.authentication.service.IssuedSession;
import com.dating.owoke.identity.shared.messaging.event.UserRegisteredV1;
import com.dating.owoke.identity.shared.messaging.event.UserTelegramLinkedV1;
import com.dating.owoke.identity.shared.messaging.service.OutboxService;
import com.dating.owoke.identity.telegram.domain.ExternalIdentity;
import com.dating.owoke.identity.telegram.domain.ExternalProvider;
import com.dating.owoke.identity.telegram.repository.ExternalIdentityRepository;

@Service
public class TelegramIdentityService {

    private static final String IDENTITY_EVENTS_TOPIC = "identity.events.v1";

    private final ExternalIdentityRepository identityRepository;
    private final UserAccountRepository userRepository;
    private final AuthenticationService authenticationService;
    private final OutboxService outboxService;
    private final Clock clock;

    public TelegramIdentityService(
            ExternalIdentityRepository identityRepository,
            UserAccountRepository userRepository,
            AuthenticationService authenticationService,
            OutboxService outboxService,
            Clock clock) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional
    public IssuedSession authenticate(TelegramProfile profile) {
        ExternalIdentity identity = identityRepository
                .findByProviderAndSubject(ExternalProvider.TELEGRAM, profile.subject())
                .orElse(null);

        UserAccount account;
        if (identity == null) {
            if (identityRepository.findByTelegramUserId(profile.telegramUserId()).isPresent()) {
                throw new AuthenticationRejectedException("Telegram identity is already linked");
            }
            account = userRepository.save(UserAccount.registerExternal(profile.displayName(), clock.instant()));
            identity = identityRepository.save(new ExternalIdentity(
                    account.getId(),
                    profile.subject(),
                    profile.telegramUserId(),
                    profile.username(),
                    profile.botAccess(),
                    clock.instant()));
            outboxService.enqueue(
                    IDENTITY_EVENTS_TOPIC,
                    account.getId().toString(),
                    "UserRegisteredV1",
                    new UserRegisteredV1(account.getId(), account.getDisplayName(), null));
            publishLinked(account, identity);
        } else {
            identity.recordLogin(profile.username(), profile.botAccess(), clock.instant());
            account = userRepository.findById(identity.getUserId())
                    .orElseThrow(() -> new IllegalStateException("External identity has no user"));
        }

        if (!account.isActive()) {
            throw new AuthenticationRejectedException("Account is unavailable");
        }
        return authenticationService.issueSession(account);
    }

    private void publishLinked(UserAccount account, ExternalIdentity identity) {
        outboxService.enqueue(
                IDENTITY_EVENTS_TOPIC,
                account.getId().toString(),
                "UserTelegramLinkedV1",
                new UserTelegramLinkedV1(
                        account.getId(),
                        identity.getTelegramUserId(),
                        identity.getUsername(),
                        identity.hasBotAccess()));
    }
}
