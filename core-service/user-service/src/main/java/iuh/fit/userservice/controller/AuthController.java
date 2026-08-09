package iuh.fit.userservice.controller;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.response.ApiResponse;
import iuh.fit.userservice.dto.request.*;
import iuh.fit.userservice.dto.response.LoginResponse;
import iuh.fit.userservice.dto.response.UserInfoResponse;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.redis.OtpRedisService;
import iuh.fit.userservice.redis.TokenRedisService;
import iuh.fit.userservice.service.AuthService;
import iuh.fit.userservice.service.UserService;
import iuh.fit.userservice.utils.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final TokenRedisService tokenRedisService;
    private final OtpRedisService otpRedisService;

    @PostMapping("/register/citizen")
    public ResponseEntity<ApiResponse<?>> registerCitizen(@Valid @RequestBody CitizenRegisterRequest request) {
        User user = authService.registerCitizen(request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/register/rescuer")
    public ResponseEntity<ApiResponse<?>> registerRescuer(@Valid @RequestBody VolunteerRegisterRequest request) {
        User user = authService.registerRescuer(request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<?>> verifyOtp(@Valid @RequestBody OtpVerificationRequest request){
        return ResponseEntity.ok(ApiResponse.success(authService.verifyOtp(request)));
    }

    @PostMapping("/login")
    public  ResponseEntity<ApiResponse<?>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client-Type", defaultValue = "WEB") String clientType,
            HttpServletResponse response
    ){
        UserInfoResponse userInfoResponse = authService.login(request);
        String accessToken = jwtUtils.generateAccessToken(userInfoResponse);
        String refreshToken = jwtUtils.generateRefreshToken(userInfoResponse);
        if ("MOBILE".equalsIgnoreCase(clientType)) {
            LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken);
            return ResponseEntity.ok(ApiResponse.success(loginResponse));
        }
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .build();
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<?>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            @RequestHeader(value = "x-refresh-token", required = false) String headerRefreshToken
    ){
        final String actualRefreshToken = StringUtils.hasText(cookieRefreshToken)
                ? cookieRefreshToken
                : headerRefreshToken;

        if(actualRefreshToken == null || actualRefreshToken.trim().isEmpty()){
            throw new BusinessException(ErrorCode.FORBIDDEN, "Refresh token is missing");
        }
        boolean isTokenExpired = jwtUtils.isTokenExpired(actualRefreshToken);
        UUID userId = jwtUtils.extractId(actualRefreshToken);


        if (userId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Invalid token payload: Missing User ID");
        }

        System.out.println("User ID from refresh token: " + userId);

        if(isTokenExpired){
            throw new BusinessException(ErrorCode.FORBIDDEN, "Refresh token is expired");
        }

        if(!userService.checkUserExist(userId)){
            throw new BusinessException(ErrorCode.FORBIDDEN, "User not found");
        }
        UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                .id(userId)
                .fullName(jwtUtils.extractFullName(actualRefreshToken))
                .role(jwtUtils.extractRole(actualRefreshToken))
                .phone(jwtUtils.extractPhone(actualRefreshToken))
                .build();
        String accessToken = jwtUtils.generateAccessToken(userInfoResponse);

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .build();
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String bearerToken,

            // Lấy Refresh Token từ Cookie (Web) hoặc Custom Header (Mobile)
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            @RequestHeader(value = "x-refresh-token", required = false) String headerRefreshToken,

            HttpServletResponse response
    ) {
        String accessToken = null;
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            accessToken = bearerToken.substring(7);
        }

        // 2. Trích xuất Refresh Token
        final String refreshToken = StringUtils.hasText(cookieRefreshToken)
                ? cookieRefreshToken
                : headerRefreshToken;

        Long accessTokenRemainingTime = jwtUtils.extractTimeRemaining(accessToken);
        Long refreshTokenRemainingTime = jwtUtils.extractTimeRemaining(refreshToken);

        UUID userIdFromAccessToken = jwtUtils.extractId(accessToken);
        UUID userIdFromRefreshToken = jwtUtils.extractId(refreshToken);

        tokenRedisService.revokeTokens("accessToken:" + userIdFromAccessToken,accessToken,accessTokenRemainingTime);
        tokenRedisService.revokeTokens("refreshToken:" + userIdFromRefreshToken ,refreshToken,refreshTokenRemainingTime);


        // 4. Xóa Cookie ở trình duyệt (cho Web)
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());



        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }


    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<ApiResponse<?>>  forgotPassword(@Validated @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        UserInfoResponse userInfoResponse = authService.forgotPasswordRequest(forgotPasswordRequest.getPhoneNumber());
        if(userInfoResponse != null){
            return ResponseEntity.ok(ApiResponse.success(userInfoResponse, "OTP sent successfully"));
        }
        return null;
    }

    @PostMapping("/forgot-password/verify-otp")
    public ApiResponse<?> forgotVerifyOtp(@Validated @RequestBody OtpVerificationRequest otpVerificationRequest) {
        boolean isValidOtp = otpRedisService.verifyOtp("opt:" + otpVerificationRequest.getId(), otpVerificationRequest.getOtp());
        if(!isValidOtp) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid OTP");
        }
        String resetToken = jwtUtils.generateResetToken( otpVerificationRequest.getPhoneNumber(), otpVerificationRequest.getId());

        return ApiResponse.success(resetToken, "OTP verified successfully");
    }

    @PostMapping("/forgot-password/reset-password")
    public ApiResponse<?> resetPassword(
            @Validated @RequestBody ResetPasswordRequest resetPasswordRequest,
            @RequestHeader(value = "reset-token", required = true) String resetToken
    ) {
        if(jwtUtils.isTokenExpired(resetToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Reset token is expired");
        }
        UUID userId = jwtUtils.extractId(resetToken);
        boolean isReset = authService.resetPassword(userId, resetPasswordRequest);
        if(isReset) {
            return ApiResponse.success(true, "Password reset successfully");
        }

        return null;
    }


}
