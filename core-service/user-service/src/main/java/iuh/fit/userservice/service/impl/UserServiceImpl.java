package iuh.fit.userservice.service.impl;

import iuh.fit.userservice.repository.UserRepository;
import iuh.fit.userservice.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public boolean checkUserExist(UUID userId) {
        return userRepository.existsById(userId);
    }
}
