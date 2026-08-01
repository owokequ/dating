package com.dating.owoke.places.sync.configuration;

import java.util.Set;

public enum KudaGoCollection {
    FOOD(Set.of("restaurants", "anticafe")),
    LEISURE(Set.of("attractions", "cinema", "museums", "park", "theatre"));

    private final Set<String> categories;

    KudaGoCollection(Set<String> categories) {
        this.categories = categories;
    }

    public Set<String> categories() {
        return categories;
    }

    public String queryValue() {
        return categories.stream().sorted().collect(java.util.stream.Collectors.joining(","));
    }

    public int limit(KudaGoProperties properties) {
        return this == FOOD ? properties.foodLimit() : properties.leisureLimit();
    }
}
