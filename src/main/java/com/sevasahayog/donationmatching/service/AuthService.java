package com.sevasahayog.donationmatching.service;

import com.sevasahayog.donationmatching.dto.AuthResponse;
import com.sevasahayog.donationmatching.dto.LoginRequest;
import com.sevasahayog.donationmatching.dto.UserRegisterRequest;
import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.exception.DuplicateEmailException;
import com.sevasahayog.donationmatching.repository.UserRepository;
import com.sevasahayog.donationmatching.security.JwtService;
import com.sevasahayog.donationmatching.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(UserRegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.DONOR)
                .active(true)
                .build();
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(email);
        }
        return buildAuthResponse(UserPrincipal.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        return buildAuthResponse((UserPrincipal) authentication.getPrincipal());
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal) {
        String token = jwtService.generateToken(principal.getUsername());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(),
                principal.getId(), principal.getUsername(), principal.getRole());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
