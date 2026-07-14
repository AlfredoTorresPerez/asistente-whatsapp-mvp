package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.io.Serializable;

@Component("permissionEvaluator")
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || authentication.getPrincipal() == null || permission == null) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authUser) {
            return authUser.hasPermission(permission.toString());
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }
}