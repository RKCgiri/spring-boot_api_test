package com.example.app.service.impl;

import com.example.app.dto.request.*;
import com.example.app.dto.response.*;
import com.example.app.enums.Role;
import com.example.app.entity.User;
import com.example.app.enums.UserStatus;
import com.example.app.service.UserService;
import com.example.app.exception.DuplicateResourceException;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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
    // STATS
    // -----------------------------------------------------------------------
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

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Wrap in quotes if value contains comma, quote or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
