package iuh.fit.userservice.controller;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.response.ApiResponse;
import iuh.fit.userservice.dto.request.*;
import iuh.fit.userservice.dto.response.LoginResponse;
import iuh.fit.userservice.dto.response.ResetTokenResponse;
import iuh.fit.userservice.dto.response.UserInfoResponse;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.redis.OtpRedisService;
import iuh.fit.userservice.redis.TokenRedisService;
import iuh.fit.userservice.service.AuthService;
import iuh.fit.userservice.service.UserService;
import iuh.fit.userservice.utils.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final TokenRedisService tokenRedisService;
    private final OtpRedisService otpRedisService;

    @Value("${app.security.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> registerCitizen(@Valid @RequestBody CitizenRegisterRequest request) {
        User user = authService.registerCitizen(request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<?>> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.verifyOtp(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client-Type", defaultValue = "WEB") String clientType,
            HttpServletResponse response
    ) {
        UserInfoResponse userInfoResponse = authService.login(request);
        String accessToken = jwtUtils.generateAccessToken(userInfoResponse);
        String refreshToken = jwtUtils.generateRefreshToken(userInfoResponse);
        if ("MOBILE".equalsIgnoreCase(clientType)) {
            LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken, userInfoResponse);
            return ResponseEntity.ok(ApiResponse.success(loginResponse));
        }
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite(cookieSecure ? "None" : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userInfoResponse(userInfoResponse)
                .build();
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<?>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            @RequestHeader(value = "x-refresh-token", required = false) String headerRefreshToken,
            HttpServletResponse response
    ) {
        final String actualRefreshToken = StringUtils.hasText(cookieRefreshToken)
                ? cookieRefreshToken
                : headerRefreshToken;

        if (actualRefreshToken == null || actualRefreshToken.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is missing");
        }

        try {
            if (jwtUtils.isTokenExpired(actualRefreshToken)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is expired");
            }

            UUID userId = jwtUtils.extractId(actualRefreshToken);
            if (userId == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid token payload: Missing User ID");
            }

            if (tokenRedisService.isTokenRevoked("refreshToken:" + userId, actualRefreshToken)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token has been revoked");
            }

            if (!userService.checkUserExist(userId)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "User not found");
            }

            UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                    .id(userId)
                    .teamId(jwtUtils.extractTeamId(actualRefreshToken))
                    .fullName(jwtUtils.extractFullName(actualRefreshToken))
                    .role(jwtUtils.extractRole(actualRefreshToken))
                    .phone(jwtUtils.extractPhone(actualRefreshToken))
                    .build();
            String accessToken = jwtUtils.generateAccessToken(userInfoResponse);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", actualRefreshToken)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60)
                    .sameSite(cookieSecure ? "None" : "Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            LoginResponse loginResponse = LoginResponse.builder()
                    .accessToken(accessToken)
                    .userInfoResponse(userInfoResponse)
                    .refreshToken(actualRefreshToken)
                    .build();
            return ResponseEntity.ok(ApiResponse.success(loginResponse));
        } catch (io.jsonwebtoken.JwtException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid refresh token: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearerToken,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            @RequestHeader(value = "x-refresh-token", required = false) String headerRefreshToken,
            HttpServletResponse response
    ) {
        String accessToken = null;
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            accessToken = bearerToken.substring(7);
        }

        final String refreshToken = StringUtils.hasText(cookieRefreshToken)
                ? cookieRefreshToken
                : headerRefreshToken;

        Long accessTokenRemainingTime = jwtUtils.extractTimeRemaining(accessToken);
        Long refreshTokenRemainingTime = jwtUtils.extractTimeRemaining(refreshToken);

        UUID userIdFromAccessToken = jwtUtils.extractId(accessToken);
        UUID userIdFromRefreshToken = jwtUtils.extractId(refreshToken);

        tokenRedisService.revokeTokens("accessToken:" + userIdFromAccessToken, accessToken, accessTokenRemainingTime);
        tokenRedisService.revokeTokens("refreshToken:" + userIdFromRefreshToken, refreshToken, refreshTokenRemainingTime);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSecure ? "None" : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@Validated @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        UserInfoResponse userInfoResponse = authService.forgotPasswordRequest(forgotPasswordRequest.getPhoneNumber());
        if (userInfoResponse != null) {
            return ResponseEntity.ok(ApiResponse.success(userInfoResponse, "OTP sent successfully"));
        }
        return null;
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponse<?>> forgotVerifyOtp(@Validated @RequestBody OtpVerificationRequest otpVerificationRequest) {
        boolean isValidOtp = otpRedisService.verifyOtp("opt:" + otpVerificationRequest.getId(), otpVerificationRequest.getOtp());
        if (!isValidOtp) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid OTP");
        }
        String resetToken = jwtUtils.generateResetToken(otpVerificationRequest.getPhoneNumber(), otpVerificationRequest.getId());
        ResetTokenResponse resetTokenResponse = ResetTokenResponse.builder()
                .resetToken(resetToken)
                .build();
        return ResponseEntity.ok(ApiResponse.success(resetTokenResponse, "OTP verified successfully"));
    }

    @PostMapping("/forgot-password/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(
            @Validated @RequestBody ResetPasswordRequest resetPasswordRequest,
            @RequestHeader(value = "reset-token", required = true) String resetToken
    ) {
        if (jwtUtils.isTokenExpired(resetToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Reset token is expired");
        }
        UUID userId = jwtUtils.extractId(resetToken);
        boolean isReset = authService.resetPassword(userId, resetPasswordRequest);
        if (isReset) {
            return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
        }
        return null;
    }

    @GetMapping("/user-info")
    public ResponseEntity<ApiResponse<?>> getUserInfo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) String bearerToken
    ) {
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String accessToken = bearerToken.substring(7);
        if (jwtUtils.isTokenExpired(accessToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access token is expired");
        }
        UUID userId = jwtUtils.extractId(accessToken);
        UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                .id(userId)
                .teamId(jwtUtils.extractTeamId(accessToken))
                .fullName(jwtUtils.extractFullName(accessToken))
                .role(jwtUtils.extractRole(accessToken))
                .phone(jwtUtils.extractPhone(accessToken))
                .build();
        return ResponseEntity.ok(ApiResponse.success(userInfoResponse));
    }
}
