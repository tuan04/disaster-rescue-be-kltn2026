package iuh.fit.userservice.service;

import java.util.UUID;

public interface UserService {
    public boolean checkUserExist(UUID userId);
}
