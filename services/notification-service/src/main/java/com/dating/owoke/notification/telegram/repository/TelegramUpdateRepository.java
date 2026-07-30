package com.dating.owoke.notification.telegram.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.notification.telegram.domain.TelegramUpdate;

public interface TelegramUpdateRepository extends JpaRepository<TelegramUpdate, Long> {
}
