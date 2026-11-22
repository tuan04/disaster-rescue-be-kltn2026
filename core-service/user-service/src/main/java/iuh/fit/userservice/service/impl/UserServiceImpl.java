package iuh.fit.userservice.service.impl;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.CCCDImageGrpcResponse;
import iuh.fit.userservice.dto.request.*;
import iuh.fit.common.response.PageResponse;
import iuh.fit.userservice.dto.request.AdminUserRequest;
import iuh.fit.userservice.dto.request.AdminVolunteerProfileRequest;
import iuh.fit.userservice.dto.request.UserFilterRequest;
import iuh.fit.userservice.dto.response.AdminUserResponse;
import iuh.fit.userservice.dto.response.UserIDAndNameResponse;
import iuh.fit.userservice.dto.response.UserListResponse;
import iuh.fit.userservice.dto.response.UserProfileResponse;
import iuh.fit.userservice.entity.User;

import iuh.fit.userservice.entity.VolunteerProfile;
import iuh.fit.userservice.enums.AccountRole;
import iuh.fit.userservice.enums.RoleEnum;
import iuh.fit.userservice.enums.RoleInTeamEnum;
import iuh.fit.userservice.enums.VerifiedStatusEnum;
import iuh.fit.userservice.grpc.IntegrationGrpcClient;
import iuh.fit.userservice.repository.UserRepository;
import iuh.fit.userservice.service.UserService;
import iuh.fit.userservice.specification.UserSpecification;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final IntegrationGrpcClient integrationGrpcClient;

    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;

    @Override
    public boolean checkUserExist(UUID userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional
    public boolean upgradeToRescuerRequest(UUID userId,MultipartFile file) {
        User user = userRepository.findById(userId).orElse(null);
        if(user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found");
        }

        CCCDImageGrpcResponse request = integrationGrpcClient.extractCccd(file);


        System.out.println("CCCD Image Grpc Response: " + request);


        if (user.getRole() == RoleEnum.EMPLOYEE
                && user.getVolunteerProfile() != null
                && user.getVolunteerProfile().getVerifiedStatus() == VerifiedStatusEnum.VERIFIED) {
            throw new BusinessException(ErrorCode.CONFLICT, "User is already an employee");
        }

        String birthDay = request.getBirthDay().strip();

        LocalDate birthDate = LocalDate.parse(birthDay);
        user.setBirthDate(birthDate);

        user.setFullName(request.getFullName());

        VolunteerProfile volunteerProfile = user.getVolunteerProfile();
        if (volunteerProfile == null) {
            volunteerProfile = VolunteerProfile.builder()
                    .user(user)
                    .cccdNumber(request.getCCCDNumber())
                    .address(request.getAddress())
                    .verifiedStatus(VerifiedStatusEnum.PENDING)
                    .build();
            user.setVolunteerProfile(volunteerProfile);
        } else {
            volunteerProfile.setCccdNumber(request.getCCCDNumber());
            volunteerProfile.setVerifiedStatus(VerifiedStatusEnum.PENDING);
        }
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean upgradeToRescuerRequestAccept(UUID id) {
        User user = findUserById(id);
        if (user.getVolunteerProfile() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Volunteer profile not found");
        }
        user.setRole(RoleEnum.EMPLOYEE);
        user.getVolunteerProfile().setCurrentRoleInTeam(RoleInTeamEnum.RESCUER);
        user.getVolunteerProfile().setVerifiedStatus(VerifiedStatusEnum.VERIFIED);
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = findUserById(userId);
        return UserProfileResponse.fromEntity(user);
    }

    @Override
    public List<UserIDAndNameResponse> getUserNames() {
        return userRepository.findByVolunteerProfile_VerifiedStatusAndVolunteerProfile_CurrentRoleInTeam(VerifiedStatusEnum.VERIFIED, RoleInTeamEnum.COORDINATOR)
                .stream()
                .map(user -> new UserIDAndNameResponse(user.getId(), user.getCode(), user.getFullName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserListResponse> getAllUsersPage(UserFilterRequest request, Pageable pageable) {
        Specification<User> spec = UserSpecification.search(request);
        Page<UserListResponse> userPage = userRepository.findAll(spec, pageable)
                .map(UserListResponse::fromEntity);
        return PageResponse.from(userPage);
    }



    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }


    @Override
    @Transactional
    public AdminUserResponse createUserByAdmin(AdminUserRequest request) {
        validateAdminRequest(request);

        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Password is required when creating a user"
            );
        }

        if (userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Phone number already exists"
            );
        }

        User user = new User();
        applyAdminRequest(user, request);

        return saveAdminUser(user);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(
            UUID userId,
            AdminUserRequest request
    ) {
        validateAdminRequest(request);

        // Admin được tìm cả tài khoản đã xóa mềm để khôi phục.
        User user = findUserById(userId);

        boolean phoneChanged = !user.getPhone().equals(request.phone());

        if (phoneChanged && userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Phone number already exists"
            );
        }

        applyAdminRequest(user, request);

        return saveAdminUser(user);
    }


    private void validateAdminRequest(AdminUserRequest request) {
        boolean employee = request.role() != AccountRole.CITIZEN;

        if (employee && request.volunteerProfile() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Volunteer profile is required for this role"
            );
        }

    }

    private void applyAdminRequest(
            User user,
            AdminUserRequest request
    ) {
        user.setPhone(request.phone());
        user.setFullName(request.fullName().strip());
        user.setSex(request.sex());
        user.setAvatarUrl(request.avatarUrl());
        user.setBirthDate(request.birthDate());
        user.setDeleted(request.deleted());
        user.setValidated(request.validated());

        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        boolean employee = request.role() != AccountRole.CITIZEN;

        user.setRole(employee ? RoleEnum.EMPLOYEE : RoleEnum.CITIZEN);

        VolunteerProfile profile = user.getVolunteerProfile();
        AdminVolunteerProfileRequest profileRequest =
                request.volunteerProfile();

        if (profileRequest != null) {
            if (profile == null) {
                profile = new VolunteerProfile();
                profile.setUser(user);
                user.setVolunteerProfile(profile);
            }

            profile.setCccdNumber(profileRequest.cccdNumber());
            profile.setDescription(profileRequest.description());
            profile.setAddress(profileRequest.address());
            profile.setVerifiedStatus(profileRequest.verifiedStatus());
        }

        if (profile != null) {
            profile.setCurrentRoleInTeam(
                    employee
                            ? RoleInTeamEnum.valueOf(request.role().name())
                            : null
            );
        }
    }

    private AdminUserResponse saveAdminUser(User user) {
        try {
            User savedUser = userRepository.saveAndFlush(user);
            return AdminUserResponse.fromEntity(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "User data conflicts with existing data"
            );
        }
    }



    @Override
    public UserProfileResponse updateMyProfile(
            UUID userId,
            UpdateMyProfileRequest request
    ) {
        User user = findUserById(userId);

        if (user.isDeleted()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy người dùng"
            );
        }

        if (request.getFullName() != null) {
            String fullName = request.getFullName().strip();

            if (fullName.isBlank()) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "Họ tên không được để trống"
                );
            }

            user.setFullName(fullName);
        }

        if (request.getSex() != null) {
            user.setSex(request.getSex());
        }

        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }

        if (request.getAvatar() != null) {
            String avatarUrl = s3Service.uploadAvatar(
                    userId,
                    request.getAvatar()
            );

            user.setAvatarUrl(avatarUrl);
        }

        User savedUser = userRepository.save(user);

        return UserProfileResponse.fromEntity(savedUser);
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (user.isDeleted()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy người dùng"
            );
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Xác nhận mật khẩu mới không khớp"
            );
        }

        // BCrypt giới hạn mật khẩu theo byte, không phải số ký tự.
        if (request.newPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Mật khẩu mới không được vượt quá 72 byte UTF-8"
            );
        }

        if (!passwordEncoder.matches(
                request.oldPassword(),
                user.getPassword()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Mật khẩu cũ không chính xác"
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPassword()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Mật khẩu mới phải khác mật khẩu cũ"
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.newPassword())
        );

        userRepository.save(user);
    }


    @Transactional(readOnly = true)
    public Slice<UUID> getUserIds(int page, int size) {
        return userRepository.findNotificationUserIds(
                PageRequest.of(page, size)
        );
    }

}
