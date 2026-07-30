package com.dating.owoke.dating.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorWriter {

    public void unauthorized(
            jakarta.servlet.http.HttpServletRequest request,
            HttpServletResponse response,
            Exception exception
    ) throws IOException {
        write(response, 401, "Unauthorized", "Authentication is required");
    }

    public void forbidden(
            jakarta.servlet.http.HttpServletRequest request,
            HttpServletResponse response,
            Exception exception
    ) throws IOException {
        write(response, 403, "Forbidden", "Access is denied");
    }

    private static void write(HttpServletResponse response, int status, String title, String detail) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s"}
                """.formatted(title, status, detail));
    }
}
