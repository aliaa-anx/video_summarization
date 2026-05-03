package com.backend_microservices.auth_service.config;

import com.backend_microservices.auth_service.client.AuditClient;
import com.backend_microservices.auth_service.dto.AuditLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//  this filter intercepts every HTTP request before it reaches the controller
//  Request -> AuditLoggingFilter -> Security Filters -> Controller -> Response -> AuditLoggingFilter (completion)
@Component
@RequiredArgsConstructor
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditClient auditClient;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ofcourse we  shouldn't log ourselves, logging the log will keep us in a loop ~_~
        if (request.getRequestURI().contains("/audit")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String userId = request.getHeader("X-User-Id");
        String uri = request.getRequestURI();
        String action = resolveAction(uri);

        try {
            filterChain.doFilter(request, response);  // this processes the actual request

            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setServiceName("ai-service");
            log.setEndpoint(uri);
            log.setMethod(request.getMethod());
            log.setStatus(String.valueOf(response.getStatus()));
            log.setAction(action);
            log.setDetails("Success in " + (System.currentTimeMillis() - startTime) + "ms"); // any filler words...

            auditClient.sendLog(log);   // here is where we communicate with the audit-service to save our logs

        } catch (Exception ex) {    // in case if error happens

            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setServiceName("ai-service");
            log.setEndpoint(uri);
            log.setMethod(request.getMethod());
            log.setStatus("FAIL");
            log.setAction(action);
            log.setDetails(ex.getMessage());    // error message

            auditClient.sendLog(log);

            throw ex;
        }
    }

    // sets the action depending on the request
    private String resolveAction(String uri) {
        if (uri.contains("register")) {
            return "USER_REGISTER";
        } else if (uri.contains("login")) {
            return "USER_LOGIN";
        } else if (uri.contains("forgot") && uri.contains("password")) {
            return "USER_FORGOT_PASSWORD";
        } else if (uri.contains("reset") && uri.contains("password")) {
            return "RESET_PASSWORD";
        }  else {
            return "GENERAL";
        }
    }
}