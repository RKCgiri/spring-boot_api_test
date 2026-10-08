package com.example.app.service.impl;

import com.example.app.dto.request.*;
import com.example.app.dto.response.*;
import com.example.app.enums.Role;
import com.example.app.entity.User;
import com.example.app.enums.UserStatus;
import com.example.app.service.UserService;
import com.example.app.exception.DuplicateResourceException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Business logic for user management.
 * <p>
 * Covers:
 * <ul>
 *   <li>CRUD operations</li>
 *   <li>Paginated search with filters (search text, role, status, school, region)</li>
 *   <li>Summary statistics for the dashboard cards</li>
 *   <li>CSV export data retrieval</li>
 *   <li>Last-login timestamp update</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    /**
     * Creates a new user from the given request.
     *
     * @param request Validated request body.
     * @return The persisted user as a response DTO.
     * @throws DuplicateResourceException if a user with the same email already exists.
     */
    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.debug("Creating user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                "A user with email '" + request.email() + "' already exists");
        }

        User user = User.builder()
            .firstName(request.firstName())
            .lastName(request.lastName())
            .email(request.email())
            .phone(request.phone())
            .role(request.role())
            .status(request.status() != null ? request.status() : UserStatus.ACTIVE)
            .school(request.school())
            .region(request.region())
            .permissions(request.permissions() != null ? request.permissions() : Collections.emptySet())
            .password(request.password())
            .build();

        User saved = userRepository.save(user);
        log.info("Created user id={} email={}", saved.getId(), saved.getEmail());
        return UserResponse.from(saved);
    }

    // -----------------------------------------------------------------------
    // READ – single user
    // -----------------------------------------------------------------------

    /**
     * Retrieves a single user by primary key.
     *
     * @param id User ID.
     * @return User response DTO.
     * @throws ResourceNotFoundException if no user with the given ID exists.
     */
    public UserResponse getUserById(Long id) {
        User user = findOrThrow(id);
        return UserResponse.from(user);
    }

    // -----------------------------------------------------------------------
    // READ – paginated list with filters
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated, filtered list of users.
     *
     * @param search Free-text search across name, email and phone.
     * @param role   Optional role filter.
     * @param status Optional status filter.
     * @param school Optional school filter (exact match, case-insensitive).
     * @param region Optional region filter (exact match, case-insensitive).
     * @param page   0-based page index.
     * @param size   Page size (default 20, max 100).
     */
    public PagedResponse<UserResponse> getUsers(
        String search,
        Role role,
        UserStatus status,
        String school,
        String region,
        int page,
        int size
    ) {
        size = Math.min(size, 100);   // cap page size
        Pageable pageable = PageRequest.of(page, size);

        // Pass null instead of blank strings so JPQL IS NULL checks work
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String schoolParam = (school != null && !school.isBlank()) ? school.trim() : null;
        String regionParam = (region != null && !region.isBlank()) ? region.trim() : null;

        Page<User> result = userRepository.findWithFilters(
            searchParam, role, status, schoolParam, regionParam, pageable);

        List<UserResponse> content = result.getContent()
            .stream()
            .map(UserResponse::from)
            .toList();

        return new PagedResponse<>(
            content,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    /**
     * Fully replaces the fields of an existing user (PUT semantics).
     *
     * @param id      User ID.
     * @param request Validated request body.
     * @return Updated user response DTO.
     * @throws ResourceNotFoundException  if the user does not exist.
     * @throws DuplicateResourceException if the new email is already taken by another user.
     */
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        log.debug("Updating user id={}", id);
        User user = findOrThrow(id);

        // Email uniqueness check – allow keeping the same email
        if (!user.getEmail().equalsIgnoreCase(request.email())
            && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                "A user with email '" + request.email() + "' already exists");
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setStatus(request.status());
        user.setSchool(request.school());
        user.setRegion(request.region());
        if (request.permissions() != null) {
            user.getPermissions().clear();
            user.getPermissions().addAll(request.permissions());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(request.password());
        }

        User saved = userRepository.save(user);
        log.info("Updated user id={}", saved.getId());
        return UserResponse.from(saved);
    }



    // -----------------------------------------------------------------------
    // PATCH – record login timestamp
    // -----------------------------------------------------------------------

    /**
     * Sets the lastLogin timestamp to now.
     * Called internally after successful authentication (not exposed directly).
     *
     * @param id User ID.
     */
    @Transactional
    public void recordLogin(Long id) {
        User user = findOrThrow(id);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        log.debug("Recorded login for user id={}", id);
    }

    // -----------------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------------

    /**
     * Permanently deletes a user.
     *
     * @param id User ID.
     * @throws ResourceNotFoundException if the user does not exist.
     */
    @Transactional
    public void deleteUser(Long id) {
        log.debug("Deleting user id={}", id);
        User user = findOrThrow(id);
        userRepository.delete(user);
        log.info("Deleted user id={}", id);
    }

    // -----------------------------------------------------------------------
    // STATS
    // -----------------------------------------------------------------------

    /**
     * Returns aggregate statistics displayed in the four summary cards.
     */
    public UserStatsResponse getStats() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();

        return new UserStatsResponse(
            userRepository.count(),
            userRepository.countCreatedSince(startOfMonth),
            userRepository.countByRole(Role.CENTRAL_ADMIN),
            userRepository.countByRoleAndStatus(Role.CENTRAL_ADMIN, UserStatus.ACTIVE),
            userRepository.countByRole(Role.CONTENT_ADMIN),
            userRepository.countByRoleAndStatus(Role.CONTENT_ADMIN, UserStatus.ACTIVE),
            userRepository.countByRole(Role.SCHOOL_ADMIN),
            userRepository.countByRoleAndStatus(Role.SCHOOL_ADMIN, UserStatus.INACTIVE)
        );
    }

    // -----------------------------------------------------------------------
    // CSV EXPORT
    // -----------------------------------------------------------------------

    /**
     * Returns a CSV string of all users matching the given filters.
     * The caller is responsible for writing this to the HTTP response.
     *
     * @return CSV content as a String.
     */
    public String exportCsv(String search, Role role, UserStatus status,
                             String school, String region) {

        String searchParam = (search  != null && !search.isBlank())  ? search.trim()  : null;
        String schoolParam = (school  != null && !school.isBlank())  ? school.trim()  : null;
        String regionParam = (region  != null && !region.isBlank())  ? region.trim()  : null;

        List<User> users = userRepository.findAllForExport(
            searchParam, role, status, schoolParam, regionParam);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,First Name,Last Name,Email,Phone,Role,Status,School,Region,Last Login,Permissions,Created At\n");

        for (User u : users) {
            csv.append(u.getId()).append(',')
               .append(escapeCsv(u.getFirstName())).append(',')
               .append(escapeCsv(u.getLastName())).append(',')
               .append(escapeCsv(u.getEmail())).append(',')
               .append(escapeCsv(u.getPhone())).append(',')
               .append(u.getRole()).append(',')
               .append(u.getStatus()).append(',')
               .append(escapeCsv(u.getSchool())).append(',')
               .append(escapeCsv(u.getRegion())).append(',')
               .append(u.getLastLogin() != null ? u.getLastLogin() : "").append(',')
               .append(escapeCsv(u.getPermissions().toString())).append(',')
               .append(u.getCreatedAt()).append('\n');
        }

        return csv.toString();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Wrap in quotes if value contains comma, quote or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
