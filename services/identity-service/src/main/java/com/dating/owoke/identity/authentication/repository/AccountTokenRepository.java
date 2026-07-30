package com.dating.owoke.identity.authentication.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.identity.authentication.domain.AccountToken;
import com.dating.owoke.identity.authentication.domain.AccountTokenType;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {

    Optional<AccountToken> findByTokenHashAndType(String tokenHash, AccountTokenType type);

    @Modifying
    @Query("delete from AccountToken token where token.userId = :userId and token.type = :type and token.usedAt is null")
    void deleteUnused(UUID userId, AccountTokenType type);
}
