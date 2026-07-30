package com.dating.owoke.identity.authentication.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.identity.account.domain.UserAccount;
import com.dating.owoke.identity.account.repository.UserAccountRepository;
import com.dating.owoke.identity.authentication.configuration.IdentitySecurityProperties;
import com.dating.owoke.identity.authentication.domain.AccountToken;
import com.dating.owoke.identity.authentication.domain.AccountTokenType;
import com.dating.owoke.identity.authentication.domain.PasswordCredential;
import com.dating.owoke.identity.authentication.exception.AuthenticationRejectedException;
import com.dating.owoke.identity.authentication.exception.InvalidAccountTokenException;
import com.dating.owoke.identity.authentication.repository.AccountTokenRepository;
import com.dating.owoke.identity.authentication.repository.PasswordCredentialRepository;
import com.dating.owoke.identity.shared.messaging.event.EmailNotificationRequestedV1;
import com.dating.owoke.identity.shared.messaging.event.UserRegisteredV1;
import com.dating.owoke.identity.shared.messaging.service.OutboxService;

@Service
public class AuthenticationService {

    private static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
    private static final String IDENTITY_EVENTS_TOPIC = "identity.events.v1";
    private static final String NOTIFICATION_COMMANDS_TOPIC = "notification.commands.v1";

    private final UserAccountRepository userRepository;
    private final PasswordCredentialRepository credentialRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenGenerator tokenGenerator;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final OutboxService outboxService;
    private final IdentitySecurityProperties properties;
    private final Clock clock;

    public AuthenticationService(
            UserAccountRepository userRepository,
            PasswordCredentialRepository credentialRepository,
            AccountTokenRepository accountTokenRepository,
            PasswordEncoder passwordEncoder,
            SecureTokenGenerator tokenGenerator,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            OutboxService outboxService,
            IdentitySecurityProperties properties,
            Clock clock) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.accountTokenRepository = accountTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.outboxService = outboxService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void register(String email, String displayName, String password) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);
        UserAccount existing = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existing != null) {
            if (!existing.isActive()) {
                sendVerification(existing);
            }
            return;
        }

        Instant now = clock.instant();
        UserAccount account = userRepository.save(UserAccount.registerLocal(normalizedEmail, displayName, now));
        credentialRepository.save(new PasswordCredential(account.getId(), passwordEncoder.encode(password), now));
        outboxService.enqueue(
                IDENTITY_EVENTS_TOPIC,
                account.getId().toString(),
                "UserRegisteredV1",
                new UserRegisteredV1(account.getId(), account.getDisplayName(), account.getEmail()));
        sendVerification(account);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        Instant now = clock.instant();
        AccountToken token = requireToken(rawToken, AccountTokenType.EMAIL_VERIFICATION, now);
        UserAccount account = userRepository.findById(token.getUserId()).orElseThrow(InvalidAccountTokenException::new);
        token.consume(now);
        account.verifyEmail(now);
    }

    @Transactional(readOnly = true)
    public IssuedSession login(String email, String password) {
        UserAccount account = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new AuthenticationRejectedException("Invalid credentials"));
        PasswordCredential credential = credentialRepository.findById(account.getId())
                .orElseThrow(() -> new AuthenticationRejectedException("Invalid credentials"));
        if (!passwordEncoder.matches(password, credential.getPasswordHash()) || !account.isActive()) {
            throw new AuthenticationRejectedException("Invalid credentials");
        }
        return issueSession(account);
    }

    @Transactional(readOnly = true)
    public IssuedSession refresh(String refreshToken) {
        RefreshGrant rotated = refreshTokenService.rotate(refreshToken);
        UserAccount account = userRepository.findById(rotated.userId())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new AuthenticationRejectedException("Account is unavailable"));
        JwtService.AccessToken access = jwtService.issue(toAuthenticated(account));
        return new IssuedSession(access.value(), rotated.refreshToken(), access.expiresAt());
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(UserAccount::isActive)
                .ifPresent(account -> {
                    String rawToken = issueAccountToken(
                            account.getId(), AccountTokenType.PASSWORD_RESET, PASSWORD_RESET_TTL);
                    outboxService.enqueue(
                            NOTIFICATION_COMMANDS_TOPIC,
                            account.getId().toString(),
                            "PasswordResetRequestedV1",
                            new EmailNotificationRequestedV1(
                                    account.getId(),
                                    account.getEmail(),
                                    "PASSWORD_RESET",
                                    properties.webAppUrl() + "/reset-password?token=" + encode(rawToken)));
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);
        Instant now = clock.instant();
        AccountToken token = requireToken(rawToken, AccountTokenType.PASSWORD_RESET, now);
        PasswordCredential credential = credentialRepository.findById(token.getUserId())
                .orElseThrow(InvalidAccountTokenException::new);
        token.consume(now);
        credential.changePassword(passwordEncoder.encode(newPassword), now);
        refreshTokenService.revokeAll(token.getUserId());
    }

    public IssuedSession issueSession(UserAccount account) {
        JwtService.AccessToken access = jwtService.issue(toAuthenticated(account));
        RefreshGrant refresh = refreshTokenService.issue(account.getId());
        return new IssuedSession(access.value(), refresh.refreshToken(), access.expiresAt());
    }

    private String issueAccountToken(UUID userId, AccountTokenType type, Duration ttl) {
        accountTokenRepository.deleteUnused(userId, type);
        String rawToken = tokenGenerator.generate();
        Instant now = clock.instant();
        accountTokenRepository.save(new AccountToken(
                userId,
                type,
                tokenGenerator.hash(rawToken),
                now,
                now.plus(ttl)));
        return rawToken;
    }

    private void sendVerification(UserAccount account) {
        String rawToken = issueAccountToken(
                account.getId(), AccountTokenType.EMAIL_VERIFICATION, EMAIL_VERIFICATION_TTL);
        outboxService.enqueue(
                NOTIFICATION_COMMANDS_TOPIC,
                account.getId().toString(),
                "EmailVerificationRequestedV1",
                new EmailNotificationRequestedV1(
                        account.getId(),
                        account.getEmail(),
                        "EMAIL_VERIFICATION",
                        properties.webAppUrl() + "/verify-email?token=" + encode(rawToken)));
    }

    private AccountToken requireToken(String rawToken, AccountTokenType type, Instant now) {
        return accountTokenRepository.findByTokenHashAndType(tokenGenerator.hash(rawToken), type)
                .filter(token -> token.getUsedAt() == null && token.getExpiresAt().isAfter(now))
                .orElseThrow(InvalidAccountTokenException::new);
    }

    private static AuthenticatedAccount toAuthenticated(UserAccount account) {
        return new AuthenticatedAccount(
                account.getId(), account.getDisplayName(), account.getEmail(), account.getRole());
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("password must contain 12-72 UTF-8 bytes");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
