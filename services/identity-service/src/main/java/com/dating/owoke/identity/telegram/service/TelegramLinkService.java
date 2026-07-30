package com.dating.owoke.identity.telegram.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.identity.account.repository.UserAccountRepository;
import com.dating.owoke.identity.authentication.domain.AccountToken;
import com.dating.owoke.identity.authentication.domain.AccountTokenType;
import com.dating.owoke.identity.authentication.repository.AccountTokenRepository;
import com.dating.owoke.identity.authentication.service.SecureTokenGenerator;
import com.dating.owoke.identity.telegram.configuration.TelegramOidcProperties;
import com.dating.owoke.identity.telegram.dto.TelegramLinkResponse;
import com.dating.owoke.identity.telegram.exception.TelegramOidcUnavailableException;

@Service
public class TelegramLinkService {

    private static final Duration LINK_TTL = Duration.ofMinutes(10);

    private final UserAccountRepository userRepository;
    private final AccountTokenRepository tokenRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final TelegramOidcProperties properties;
    private final Clock clock;

    public TelegramLinkService(
            UserAccountRepository userRepository,
            AccountTokenRepository tokenRepository,
            SecureTokenGenerator tokenGenerator,
            TelegramOidcProperties properties,
            Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public TelegramLinkResponse create(UUID userId) {
        if (properties.botUsername() == null || properties.botUsername().isBlank()) {
            throw new TelegramOidcUnavailableException("Telegram bot username is not configured");
        }
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        tokenRepository.deleteUnused(userId, AccountTokenType.TELEGRAM_LINK);
        String rawToken = tokenGenerator.generate();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(LINK_TTL);
        tokenRepository.save(new AccountToken(
                userId,
                AccountTokenType.TELEGRAM_LINK,
                tokenGenerator.hash(rawToken),
                now,
                expiresAt));
        String username = properties.botUsername().replaceFirst("^@", "");
        return new TelegramLinkResponse(
                "https://t.me/" + username + "?start=link_" + rawToken,
                expiresAt);
    }
}
