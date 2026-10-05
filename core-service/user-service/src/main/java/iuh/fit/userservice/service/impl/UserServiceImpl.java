package iuh.fit.userservice.service.impl;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.CCCDImageGrpcResponse;
import iuh.fit.userservice.dto.request.UpgradeRescuerRequest;
import iuh.fit.userservice.dto.request.UserFilterRequest;
import iuh.fit.userservice.dto.response.UserIDAndNameResponse;
import iuh.fit.userservice.dto.response.UserListResponse;
import iuh.fit.userservice.dto.response.UserProfileResponse;
import iuh.fit.userservice.entity.User;

import iuh.fit.userservice.entity.VolunteerProfile;
import iuh.fit.userservice.enums.RoleEnum;
import iuh.fit.userservice.enums.RoleInTeamEnum;
import iuh.fit.userservice.enums.VerifiedStatusEnum;
import iuh.fit.userservice.grpc.IntegrationGrpcClient;
import iuh.fit.userservice.repository.UserRepository;
import iuh.fit.userservice.repository.VolunteerProfileRepository;
import iuh.fit.userservice.service.UserService;
import iuh.fit.userservice.specification.UserSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final IntegrationGrpcClient integrationGrpcClient;

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
                .map(user -> new UserIDAndNameResponse(user.getId(), user.getFullName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> getAllUsersPage(UserFilterRequest request, Pageable pageable) {
        Specification<User> spec = UserSpecification.search(request);
        return userRepository.findAll(spec, pageable)
                .map(UserListResponse::fromEntity);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }
}
