package iuh.fit.userservice.service;

import iuh.fit.userservice.dto.request.*;
import iuh.fit.userservice.dto.response.UserInfoResponse;
import iuh.fit.userservice.entity.User;

import java.util.UUID;

public interface AuthService {
    public User registerCitizen(CitizenRegisterRequest request);

    public User registerRescuer(VolunteerRegisterRequest request);

    public Boolean verifyOtp(OtpVerificationRequest request);

    public UserInfoResponse login(LoginRequest request);

    public UserInfoResponse forgotPasswordRequest(String phone);

    public Boolean resetPassword(UUID id, ResetPasswordRequest request);
}
