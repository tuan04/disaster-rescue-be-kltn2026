package iuh.fit.userservice.service;

import iuh.fit.userservice.dto.request.UpgradeRescuerRequest;

import java.util.UUID;

public interface UserService {
    public boolean checkUserExist(UUID userId);

    public boolean upgradeToRescuer(UpgradeRescuerRequest request);
}
