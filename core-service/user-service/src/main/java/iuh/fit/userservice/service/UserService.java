package iuh.fit.userservice.service;

import iuh.fit.userservice.dto.request.UpgradeRescuerRequest;
import iuh.fit.userservice.dto.response.UserIDAndNameResponse;
import iuh.fit.userservice.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {
    boolean checkUserExist(UUID userId);

    boolean upgradeToRescuerRequest(UUID userId,MultipartFile file);

    boolean upgradeToRescuerRequestAccept(UUID id);

    UserProfileResponse getUserProfile(UUID userId);

    List<UserIDAndNameResponse> getUserNames();
}
