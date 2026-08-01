package com.dating.owoke.events.shared.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityErrorWriter {
    public void unauthorized(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        write(response, 401, "Authentication is required");
    }
    public void forbidden(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        write(response, 403, "Access is denied");
    }
    private void write(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status); response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
