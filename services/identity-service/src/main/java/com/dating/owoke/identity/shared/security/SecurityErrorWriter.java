package com.dating.owoke.identity.shared.security;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void unauthorized(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    public void forbidden(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "Access is denied");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        ObjectNode problem = objectMapper.createObjectNode();
        problem.put("type", "about:blank");
        problem.put("title", status.getReasonPhrase());
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("instance", URI.create(request.getRequestURI()).toString());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
