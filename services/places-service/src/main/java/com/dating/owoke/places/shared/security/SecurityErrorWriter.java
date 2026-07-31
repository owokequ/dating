package com.dating.owoke.places.shared.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityErrorWriter {

    public void unauthorized(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required");
    }

    public void forbidden(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, "Access is denied");
    }

    private void write(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
