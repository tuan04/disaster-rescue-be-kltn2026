package iuh.fit.userservice.service.impl;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.userservice.dto.request.UpgradeRescuerRequest;
import iuh.fit.userservice.entity.VolunteerProfile;
import iuh.fit.userservice.enums.VerifiedStatusEnum;
import iuh.fit.userservice.repository.UserRepository;
import iuh.fit.userservice.repository.VolunteerProfileRepository;
import iuh.fit.userservice.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

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
    public boolean upgradeToRescuer(UpgradeRescuerRequest request) {
        if (userRepository.existsById(request.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User is already a rescuer");
        }
        VolunteerProfile volunteerProfile = volunteerProfileRepository.findByUserId(request.getId());
        volunteerProfile.setCccdNumber(request.getCCCD());
        volunteerProfile.setVerifiedStatus(VerifiedStatusEnum.PENDING);

        return true;
    }
}
