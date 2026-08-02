package com.dating.owoke.notification.reminder.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.notification.reminder.domain.DateReminderContext;
import com.dating.owoke.notification.reminder.domain.DateReminderContextId;

public interface DateReminderContextRepository extends JpaRepository<DateReminderContext, DateReminderContextId> {
    void deleteByProposalId(UUID proposalId);
}
