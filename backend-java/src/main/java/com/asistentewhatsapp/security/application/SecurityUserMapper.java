package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.api.AuthUserResponse;
import com.asistentewhatsapp.security.api.UserProfileResponse;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.domain.UserAccountEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SecurityUserMapper {

    public AuthenticatedUser toAuthenticatedUser(
            UserAccountEntity userAccount,
            String businessName,
            List<String> roles,
            List<String> permissions) {
        return new AuthenticatedUser(
                userAccount.getId(),
                userAccount.getBusinessId(),
                businessName,
                userAccount.getFirstName(),
                userAccount.getLastName(),
                userAccount.getEmail(),
                userAccount.getTimezone(),
                roles,
                permissions);
    }

    public AuthUserResponse toAuthUserResponse(AuthenticatedUser authenticatedUser) {
        return new AuthUserResponse(
                authenticatedUser.userId(),
                authenticatedUser.firstName(),
                authenticatedUser.lastName(),
                authenticatedUser.email(),
                authenticatedUser.primaryRole(),
                authenticatedUser.businessId(),
                authenticatedUser.businessName(),
                authenticatedUser.timezone(),
                authenticatedUser.permissions());
    }

    public UserProfileResponse toUserProfileResponse(
            UserAccountEntity userAccount,
            String businessName,
            List<String> roles) {
        return new UserProfileResponse(
                userAccount.getId(),
                userAccount.getFirstName(),
                userAccount.getLastName(),
                userAccount.getEmail(),
                userAccount.getPhone(),
                userAccount.getTimezone(),
                roles.isEmpty() ? "AGENT" : roles.getFirst(),
                businessName);
    }
}

