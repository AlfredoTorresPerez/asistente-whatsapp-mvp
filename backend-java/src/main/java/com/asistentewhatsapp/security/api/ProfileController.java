package com.asistentewhatsapp.security.api;

import com.asistentewhatsapp.security.application.UserProfileService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ProfileController {

    private final UserProfileService userProfileService;

    public ProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/api/v1/users/me")
    public UserProfileResponse profile(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return userProfileService.getCurrentProfile(authenticatedUser);
    }

    @PatchMapping("/api/v1/users/me")
    public UserProfileResponse updateProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateCurrentProfile(authenticatedUser, request);
    }
}

