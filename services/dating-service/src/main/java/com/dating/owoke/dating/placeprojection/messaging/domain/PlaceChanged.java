package com.dating.owoke.dating.placeprojection.messaging.domain;

import java.util.UUID;

public interface PlaceChanged {

    UUID placeId();

    String name();

    String address();

    String status();
}
