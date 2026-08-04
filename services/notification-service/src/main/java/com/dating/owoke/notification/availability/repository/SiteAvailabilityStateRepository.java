package com.dating.owoke.notification.availability.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.notification.availability.domain.SiteAvailabilityState;

import jakarta.persistence.LockModeType;

public interface SiteAvailabilityStateRepository extends JpaRepository<SiteAvailabilityState, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from SiteAvailabilityState state where state.id = " + SiteAvailabilityState.SINGLETON_ID)
    Optional<SiteAvailabilityState> lockSingleton();
}
