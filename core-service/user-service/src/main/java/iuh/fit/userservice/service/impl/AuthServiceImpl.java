package iuh.fit.userservice.service.impl;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.userservice.dto.request.*;
import iuh.fit.userservice.dto.response.UserInfoResponse;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.entity.VolunteerProfile;
import iuh.fit.userservice.enums.RoleEnum;
import iuh.fit.userservice.redis.OtpRedisService;
import iuh.fit.userservice.repository.UserRepository;
import iuh.fit.userservice.repository.VolunteerProfileRepository;
import iuh.fit.userservice.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@AllArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpRedisService otpRedisService;

    @Override
    public User registerCitizen(CitizenRegisterRequest request) {
        validateRegister(request);

        User user = User.builder()
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .birthDate(request.getBirthDate())
                .sex(request.getSex())
                .isValidated(false)
                .role(RoleEnum.CITIZEN)
                .isDeleted(false)
                .build();
        User saveUser = userRepository.save(user);
        otpRedisService.generateAndSaveOtp("opt:" + saveUser.getId());

        return saveUser;
    }

    @Override
    public User registerRescuer(VolunteerRegisterRequest request) {
        validateRegister(request);

        User user = User.builder()
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .birthDate(request.getBirthDate())
                .sex(request.getSex())
                .isValidated(false)
                .role(RoleEnum.RESCUER)
                .isDeleted(false)
                .build();
        User savedUser = userRepository.save(user);
        VolunteerProfile volunteerProfile = VolunteerProfile.builder()
                .user(user)
                .cccdNumber(request.getCccdNumber())
                .build();
        User saveUser = volunteerProfileRepository.save(volunteerProfile).getUser();
        otpRedisService.generateAndSaveOtp("opt:" + saveUser.getId());
        return saveUser;
    }

    @Override
    public Boolean verifyOtp(OtpVerificationRequest request) {
        boolean isValidOtp = otpRedisService.verifyOtp("opt:" + request.getId(), request.getOtp());
        if (!isValidOtp) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid OTP");
        }
        User existingUser = userRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        existingUser.setValidated(true);
        userRepository.save(existingUser);
        return true;
    }

    @Override
    public UserInfoResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.getPhoneNumber());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid phone number or password");
        }
        boolean isPasswordMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!isPasswordMatch) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid phone number or password");
        }
        return UserInfoResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .role(user.getRole())
                .phone(user.getPhone())
                .build();
    }

    @Override
    public UserInfoResponse forgotPasswordRequest(String phone) {
        System.out.println("Forgot password request for phone: " + phone);
        User user = userRepository.findByPhone(phone);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Phone number not found");
        }
        otpRedisService.generateAndSaveOtp("opt:" + user.getId());
        return UserInfoResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    public Boolean resetPassword(UUID id, ResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Password and confirm password do not match");
        }
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        return true;
    }

    private void validateRegister(CitizenRegisterRequest request) {
        boolean existsByPhone = userRepository.existsByPhone(request.getPhone());
        // check if phone number already exists in the database
        if (existsByPhone) {
            throw new BusinessException(ErrorCode.CONFLICT, "Phone number already exists");
        }
        // check if password and confirm password are the same
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Password and confirm password do not match");
        }
    }

}
