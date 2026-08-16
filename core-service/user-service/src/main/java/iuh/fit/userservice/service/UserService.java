package iuh.fit.userservice.service;

import org.springframework.stereotype.Service;

import java.util.UUID;


public interface UserService {
    public boolean checkUserExist(UUID userId);
}
