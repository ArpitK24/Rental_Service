package com.rentalService.controller;

import com.rentalService.dto.UpdateUserLocationDto;
import com.rentalService.dto.UserResponseDto;
import com.rentalService.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/me/location")
    public ResponseEntity<UserResponseDto> getMyLocation(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUserLocationProfile(authentication.getName()));
    }

    @PutMapping("/me/location")
    public ResponseEntity<UserResponseDto> updateMyLocation(@RequestBody UpdateUserLocationDto dto,
                                                            Authentication authentication) {
        return ResponseEntity.ok(userService.updateCurrentUserLocation(authentication.getName(), dto));
    }
}
