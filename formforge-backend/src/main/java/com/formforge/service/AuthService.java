package com.formforge.service;

import com.formforge.dto.request.LoginRequest;
import com.formforge.dto.request.RefreshTokenRequest;
import com.formforge.dto.request.RegisterRequest;
import com.formforge.dto.response.AuthResponse;
import com.formforge.dto.response.UserResponse;
import com.formforge.entity.RefreshToken;
import com.formforge.entity.User;
import com.formforge.entity.enums.UserRole;
import com.formforge.exception.DuplicateResourceException;
import com.formforge.exception.UnauthorizedException;
import com.formforge.repository.RefreshTokenRepository;
import com.formforge.repository.UserRepository;
import com.formforge.security.JwtTokenProvider;
import com.formforge.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Create new user with CREATOR role by default
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(UserRole.CREATOR)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return generateTokens(SecurityUser.from(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        log.info("User logged in: {}", securityUser.getEmail());

        return generateTokens(securityUser);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = storedToken.getUser();
        SecurityUser securityUser = SecurityUser.from(user);

        // Delete old refresh token and create new one
        refreshTokenRepository.delete(storedToken);

        return generateTokens(securityUser);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
        log.info("User logged out");
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return UserResponse.from(user);
    }

    private AuthResponse generateTokens(SecurityUser securityUser) {
        String accessToken = tokenProvider.generateAccessToken(securityUser);
        String refreshToken = tokenProvider.generateRefreshToken(securityUser);

        // Store refresh token
        RefreshToken tokenEntity = RefreshToken.builder()
                .user(userRepository.getReferenceById(securityUser.getId()))
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(
                        tokenProvider.getRefreshTokenExpiration() / 1000))
                .build();
        refreshTokenRepository.save(tokenEntity);

        User user = userRepository.findById(securityUser.getId()).orElseThrow();

        return AuthResponse.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpiration(),
                UserResponse.from(user));
    }
}
