package com.dating.owoke.dating.userprojection.repository;
import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
import com.dating.owoke.dating.userprojection.domain.UserProfileProjection;
public interface UserProfileProjectionRepository extends JpaRepository<UserProfileProjection, UUID> {}
