package com.sevasahayog.donationmatching.controller;

import com.sevasahayog.donationmatching.dto.CurrentUserResponse;
import com.sevasahayog.donationmatching.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public CurrentUserResponse currentUser(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new CurrentUserResponse(principal.getId(), principal.getUsername(), principal.getRole());
    }
}
