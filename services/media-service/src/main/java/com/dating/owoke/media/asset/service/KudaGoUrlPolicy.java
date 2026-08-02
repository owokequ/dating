package com.dating.owoke.media.asset.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class KudaGoUrlPolicy {

    public String imageUrl(String value) {
        URI uri = normalizeKudaGo(value);
        if (!isAllowedImagePath(uri)) {
            throw new IllegalArgumentException("KudaGo image URL uses a forbidden media path");
        }
        return uri.toString();
    }

    public String sourceLink(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeExternalLink(value).toString();
    }

    private URI normalizeKudaGo(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("KudaGo URL must not be blank");
        }
        try {
            URI parsed = new URI(value.trim());
            String host = parsed.getHost() == null ? "" : parsed.getHost().toLowerCase(Locale.ROOT);
            if (!isKudaGoHost(host)) {
                throw new IllegalArgumentException("Remote media host is not allowed");
            }
            validateHttpUri(parsed, false);
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

    private URI normalizeExternalLink(String value) {
        try {
            URI parsed = new URI(value.trim());
            validateHttpUri(parsed, true);
            if (parsed.getHost() == null || parsed.getHost().isBlank()) {
                throw new IllegalArgumentException("Source link must have a host");
            }
            return parsed;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid source link", exception);
        }
    }

    private static boolean isKudaGoHost(String host) {
        return host.equals("kudago.com") || host.endsWith(".kudago.com");
    }

    private static boolean isAllowedImagePath(URI uri) {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (host.equals("media.kudago.com")) {
            return path.startsWith("/images/") || path.startsWith("/thumbs/");
        }
        return path.startsWith("/media/");
    }

    private static void validateHttpUri(URI uri, boolean allowFragment) {
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("URL must use HTTP or HTTPS");
        }
        if (uri.getPort() != -1 && uri.getPort() != 443 && uri.getPort() != 80) {
            throw new IllegalArgumentException("URL uses a forbidden port");
        }
        if (uri.getUserInfo() != null || (!allowFragment && uri.getFragment() != null)) {
            throw new IllegalArgumentException("URL contains forbidden components");
        }
    }
}
