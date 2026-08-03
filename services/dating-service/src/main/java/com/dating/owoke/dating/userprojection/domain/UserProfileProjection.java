package com.dating.owoke.dating.userprojection.domain;
import java.time.Instant; import java.util.UUID;
import jakarta.persistence.*;
@Entity @Table(name = "user_profile_projection")
public class UserProfileProjection {
 @Id private UUID userId; @Column(nullable=false,length=100) private String displayName; @Column(nullable=false) private Instant updatedAt;
 protected UserProfileProjection() {}
 public UserProfileProjection(UUID userId,String displayName,Instant updatedAt){this.userId=userId;this.displayName=displayName;this.updatedAt=updatedAt;}
 public void update(String value,Instant now){displayName=value;updatedAt=now;}
 public String getDisplayName(){return displayName;}
}
