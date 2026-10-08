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

/**
 * REST controller for the Users Management API.
 *
 * <pre>
 * GET    /api/users               – paginated, filtered list
 * POST   /api/users               – create a new user
 * GET    /api/users/{id}          – get user by ID
 * PUT    /api/users/{id}          – full update
 * PATCH  /api/users/{id}/status   – update status only
 * DELETE /api/users/{id}          – delete user
 * GET    /api/users/stats         – summary statistics (dashboard cards)
 * GET    /api/users/export/csv    – export filtered list as CSV
 * GET    /api/health              – liveness check
 * </pre>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // -----------------------------------------------------------------------
    // Health (kept from original controller)
    // -----------------------------------------------------------------------

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(new HealthResponse("UP", "Service is running"));
    }

    public record HealthResponse(String status, String message) {}

    // -----------------------------------------------------------------------
    // CREATE  POST /api/users
    // -----------------------------------------------------------------------

    /**
     * Creates a new user.
     *
     * @param request Validated user payload.
     * @return HTTP 201 with the created user body.
     */
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -----------------------------------------------------------------------
    // READ – paginated list  GET /api/users
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated, optionally filtered list of users.
     *
     * @param search Free-text search (name / email / phone).
     * @param role   Filter by role (CENTRAL_ADMIN | CONTENT_ADMIN | SCHOOL_ADMIN).
     * @param status Filter by status (ACTIVE | INACTIVE | LOCKED).
     * @param school Filter by school name (case-insensitive exact match).
     * @param region Filter by region (case-insensitive exact match).
     * @param page   0-based page index (default 0).
     * @param size   Page size (default 20, max 100).
     */
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

    // -----------------------------------------------------------------------
    // READ – single user  GET /api/users/{id}
    // -----------------------------------------------------------------------

    /**
     * Returns a single user by ID.
     *
     * @param id User primary key.
     * @return HTTP 200 with user body, or HTTP 404 if not found.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // -----------------------------------------------------------------------
    // UPDATE  PUT /api/users/{id}
    // -----------------------------------------------------------------------

    /**
     * Fully replaces all fields of an existing user (PUT semantics).
     *
     * @param id      User primary key.
     * @param request Validated updated user payload.
     * @return HTTP 200 with the updated user body.
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }



    // -----------------------------------------------------------------------
    // DELETE  DELETE /api/users/{id}
    // -----------------------------------------------------------------------

    /**
     * Permanently deletes a user.
     *
     * @param id User primary key.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // STATS  GET /api/users/stats
    // -----------------------------------------------------------------------

    /**
     * Returns aggregate statistics for the four summary cards on the management page.
     *
     * @return {@link UserStatsResponse} with total counts broken down by role and status.
     */
    @GetMapping("/users/stats")
    public ResponseEntity<UserStatsResponse> getStats() {
        return ResponseEntity.ok(userService.getStats());
    }

    // -----------------------------------------------------------------------
    // CSV EXPORT  GET /api/users/export/csv
    // -----------------------------------------------------------------------

    /**
     * Exports the filtered user list as a UTF-8 CSV file download.
     * Accepts the same filter parameters as {@code GET /api/users}.
     */
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
