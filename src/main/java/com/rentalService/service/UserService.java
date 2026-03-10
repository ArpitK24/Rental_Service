package com.rentalService.service;

import com.rentalService.dto.UserResponseDto;
import com.rentalService.model.User;
import com.rentalService.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public List<UserResponseDto> getAllUsers() {
        return users.findAll().stream()
                .map(u -> new UserResponseDto(
                        u.getId(),
                        u.getMobile(),
                        u.getRole().name(),
                        u.getName(),
                        u.getEmail(),
                        u.getAddress(),
                        u.getCity(),
                        u.getDob(),
                        u.getInterests(),
                        u.getCreatedAt(),
                        u.getUpdatedAt(),
                        u.getLastActiveAt()
                ))
                .collect(Collectors.toList());
    }
}
