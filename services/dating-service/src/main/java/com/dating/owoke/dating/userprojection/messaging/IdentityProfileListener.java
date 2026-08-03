package com.dating.owoke.dating.userprojection.messaging;
import java.time.Instant; import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;
import com.dating.owoke.dating.userprojection.domain.UserProfileProjection; import com.dating.owoke.dating.userprojection.repository.UserProfileProjectionRepository;
import tools.jackson.databind.JsonNode; import tools.jackson.databind.ObjectMapper;
@Component public class IdentityProfileListener {
 private final UserProfileProjectionRepository repository; private final ObjectMapper mapper;
 public IdentityProfileListener(UserProfileProjectionRepository repository,ObjectMapper mapper){this.repository=repository;this.mapper=mapper;}
 @KafkaListener(topics="identity.events.v1") @Transactional public void onEvent(String message){
  try { JsonNode root=mapper.readTree(message); String type=root.path("eventType").asString(); if(!type.equals("UserRegisteredV1")&&!type.equals("UserProfileUpdatedV1")) return;
   JsonNode p=root.path("payload"); UUID id=UUID.fromString(p.path("userId").asString()); String name=p.path("displayName").asString(); Instant now=Instant.now();
   repository.findById(id).ifPresentOrElse(x->x.update(name,now),()->repository.save(new UserProfileProjection(id,name,now)));
  } catch(Exception e){throw new IllegalArgumentException("Invalid identity profile event",e);}
 }
}
