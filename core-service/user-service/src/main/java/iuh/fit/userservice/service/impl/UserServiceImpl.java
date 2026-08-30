package iuh.fit.userservice.service.impl;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.userservice.dto.request.UpgradeRescuerRequest;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.entity.VolunteerProfile;
import iuh.fit.userservice.enums.RoleEnum;
import iuh.fit.userservice.enums.VerifiedStatusEnum;
import iuh.fit.userservice.repository.UserRepository;
import iuh.fit.userservice.repository.VolunteerProfileRepository;
import iuh.fit.userservice.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;

    @Override
    public boolean checkUserExist(UUID userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional
    public boolean upgradeToRescuerRequest(UpgradeRescuerRequest request) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        if (user.getRole() == RoleEnum.EMPLOYEE 
                && user.getVolunteerProfile() != null 
                && user.getVolunteerProfile().getVerifiedStatus() == VerifiedStatusEnum.VERIFIED) {
            throw new BusinessException(ErrorCode.CONFLICT, "User is already an employee");
        }

        VolunteerProfile volunteerProfile = user.getVolunteerProfile();
        if (volunteerProfile == null) {
            volunteerProfile = VolunteerProfile.builder()
                    .user(user)
                    .cccdNumber(request.getCCCD())
                    .verifiedStatus(VerifiedStatusEnum.PENDING)
                    .build();
            user.setVolunteerProfile(volunteerProfile);
        } else {
            volunteerProfile.setCccdNumber(request.getCCCD());
            volunteerProfile.setVerifiedStatus(VerifiedStatusEnum.PENDING);
        }

        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean upgradeToRescuerRequestAccept(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        if (user.getVolunteerProfile() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Volunteer profile not found");
        }
        user.setRole(RoleEnum.EMPLOYEE);
        user.getVolunteerProfile().setVerifiedStatus(VerifiedStatusEnum.VERIFIED);
        userRepository.save(user);
        return true;
    }
}
