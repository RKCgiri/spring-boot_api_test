package com.example.app.controller;

import com.example.app.dto.request.*;
import com.example.app.dto.response.*;
import com.example.app.enums.Role;
import com.example.app.enums.UserStatus;
import com.example.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users")
    public ResponseEntity<PagedResponse<UserResponse>> getUsers(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Role role,
        @RequestParam(required = false) UserStatus status,
        @RequestParam(required = false) String school,
        @RequestParam(required = false) String region,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            userService.getUsers(search, role, status, school, region, page, size));
    }

    @GetMapping("/users/status")
    public ResponseEntity<UserStatsResponse> getStats() {
        return ResponseEntity.ok(userService.getStats());
    }

    @GetMapping("/users/export/csv")
    public ResponseEntity<byte[]> exportCsv(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Role role,
        @RequestParam(required = false) UserStatus status,
        @RequestParam(required = false) String school,
        @RequestParam(required = false) String region
    ) {
        String csv = userService.exportCsv(search, role, status, school, region);
        String filename = "users-export-" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
