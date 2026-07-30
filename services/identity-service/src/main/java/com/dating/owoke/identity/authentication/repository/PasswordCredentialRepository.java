package com.dating.owoke.identity.authentication.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.identity.authentication.domain.PasswordCredential;

public interface PasswordCredentialRepository extends JpaRepository<PasswordCredential, UUID> {
}
