package com.rentalService.service;

import com.rentalService.dto.UserResponseDto;
import com.rentalService.dto.UpdateUserLocationDto;
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
                        u.getCurrentLatitude(),
                        u.getCurrentLongitude(),
                        u.getDob(),
                        u.getInterests(),
                        u.getCreatedAt(),
                        u.getUpdatedAt(),
                        u.getLastActiveAt()
                ))
                .collect(Collectors.toList());
    }

    public UserResponseDto getCurrentUserLocationProfile(String mobile) {
        User user = users.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toDto(user);
    }

    public UserResponseDto updateCurrentUserLocation(String mobile, UpdateUserLocationDto dto) {
        if (dto == null || dto.getLatitude() == null || dto.getLongitude() == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }
        validateCoordinates(dto.getLatitude(), dto.getLongitude());

        User user = users.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setCurrentLatitude(dto.getLatitude());
        user.setCurrentLongitude(dto.getLongitude());
        return toDto(users.save(user));
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90.0d || latitude > 90.0d) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude < -180.0d || longitude > 180.0d) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }

    private UserResponseDto toDto(User u) {
        return new UserResponseDto(
                u.getId(),
                u.getMobile(),
                u.getRole().name(),
                u.getName(),
                u.getEmail(),
                u.getAddress(),
                u.getCity(),
                u.getCurrentLatitude(),
                u.getCurrentLongitude(),
                u.getDob(),
                u.getInterests(),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                u.getLastActiveAt()
        );
    }
}
