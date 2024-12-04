package com.inn.cafe.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomerUsersDetailsService service;

    Claims claims = null;
    private String userName = null;

    // List of public endpoints that do not require authentication
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/user/login",
            "/user/signup",
            "/user/forgotPassword"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Check if the current request matches any public endpoint
        if (isPublicEndpoint(request.getServletPath())) {
            // Skip JWT validation for public endpoints
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the Authorization header
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // Extract the JWT token
            token = authorizationHeader.substring(7);
            userName = jwtUtil.extractUserName(token);
            claims = jwtUtil.extractAllClaims(token);
        }

        // Validate the token and set the security context if authentication is successful
        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = service.loadUserByUsername(userName);
            if (Boolean.TRUE.equals(jwtUtil.validateToken(token, userDetails))) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    // Helper method to check if the request path matches any public endpoint
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::matches);
    }

    // Checks if the current user has the "admin" role
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(String.valueOf(claims.get("role")));
    }

    // Checks if the current user has the "user" role
    public boolean isUser() {
        return "user".equalsIgnoreCase(String.valueOf(claims.get("role")));
    }

    // Retrieves the username of the currently authenticated user
    public String getCurrentUser() {
        return userName;
    }
}