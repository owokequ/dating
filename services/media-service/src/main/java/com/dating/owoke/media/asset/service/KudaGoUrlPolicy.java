package com.dating.owoke.media.asset.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class KudaGoUrlPolicy {

    public String imageUrl(String value) {
        URI uri = normalize(value);
        if (!uri.getPath().startsWith("/media/")) {
            throw new IllegalArgumentException("KudaGo image URL must point to /media/");
        }
        return uri.toString();
    }

    public String sourceLink(String value) {
        return normalize(value).toString();
    }

    private URI normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("KudaGo URL must not be blank");
        }
        try {
            URI parsed = new URI(value.trim());
            String host = parsed.getHost() == null ? "" : parsed.getHost().toLowerCase(Locale.ROOT);
            if (!(host.equals("kudago.com") || host.endsWith(".kudago.com"))) {
                throw new IllegalArgumentException("Remote media host is not allowed");
            }
            if (!("https".equalsIgnoreCase(parsed.getScheme()) || "http".equalsIgnoreCase(parsed.getScheme()))) {
                throw new IllegalArgumentException("Remote media URL must use HTTP or HTTPS");
            }
            if (parsed.getPort() != -1 && parsed.getPort() != 443 && parsed.getPort() != 80) {
                throw new IllegalArgumentException("Remote media URL uses a forbidden port");
            }
            if (parsed.getUserInfo() != null || parsed.getFragment() != null) {
                throw new IllegalArgumentException("Remote media URL contains forbidden components");
            }
            return new URI(
                    "https",
                    null,
                    host,
                    -1,
                    parsed.getPath(),
                    parsed.getQuery(),
                    null);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid KudaGo URL", exception);
        }
    }
}
