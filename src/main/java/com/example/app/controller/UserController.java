package com.example.app.controller;

import com.example.app.dto.request.UserRequest;
import com.example.app.dto.response.PagedResponse;
import com.example.app.dto.response.UserResponse;
import com.example.app.dto.response.UserStatsResponse;
import com.example.app.enums.Role;
import com.example.app.enums.UserStatus;
import com.example.app.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping("/users")
    @PreAuthorize("hasRole('CENTRAL_ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('CENTRAL_ADMIN', 'CONTENT_ADMIN')")
    public ResponseEntity<PagedResponse<UserResponse>> getUsers(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Role role,
        @RequestParam(required = false) UserStatus status,
        @RequestParam(required = false) String school,
        @RequestParam(required = false) String region,
        @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must not be negative") int page,
        @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size must not exceed 100") int size
    ) {
        return ResponseEntity.ok(
            userService.getUsers(search, role, status, school, region, page, size));
    }

    @GetMapping("/users/status")
    @PreAuthorize("hasAnyRole('CENTRAL_ADMIN', 'CONTENT_ADMIN')")
    public ResponseEntity<UserStatsResponse> getStats() {
        return ResponseEntity.ok(userService.getStats());
    }

    @GetMapping("/users/export/csv")
    @PreAuthorize("hasRole('CENTRAL_ADMIN')")
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
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
