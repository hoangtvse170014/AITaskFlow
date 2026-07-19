package com.taskflow.security;

import com.taskflow.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        return null;
    }

    public java.util.UUID getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
            && authentication.getPrincipal() instanceof User;
    }
}
