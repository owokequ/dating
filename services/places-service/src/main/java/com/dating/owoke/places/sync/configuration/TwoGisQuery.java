package com.dating.owoke.places.sync.configuration;

import java.util.Locale;

public record TwoGisQuery(String query, String category) {

    public TwoGisQuery {
        query = requireText(query, "query");
        category = requireText(category, "category").toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("2GIS " + name + " must not be blank");
        }
        return value.trim();
    }
}
