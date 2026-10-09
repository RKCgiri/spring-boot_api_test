package com.example.app.repository;

import com.example.app.enums.Role;
import com.example.app.entity.User;
import com.example.app.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        WHERE (:search IS NULL
                OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.email)     LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR u.phone            LIKE CONCAT('%', CAST(:search AS string), '%'))
        AND (:role   IS NULL OR u.role   = :role)
        AND (:status IS NULL OR u.status = :status)
        AND (:school IS NULL OR LOWER(u.school) = LOWER(CAST(:school AS string)))
        AND (:region IS NULL OR LOWER(u.region) = LOWER(CAST(:region AS string)))
        ORDER BY u.createdAt DESC
        """)
    Page<User> findWithFilters(
        @Param("search") String search,
        @Param("role")   Role role,
        @Param("status") UserStatus status,
        @Param("school") String school,
        @Param("region") String region,
        Pageable pageable
    );

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, UserStatus status);

    long countByStatus(UserStatus status);

    /** Users created since the given timestamp (for "N this month" badge). */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countCreatedSince(@Param("since") LocalDateTime since);

    // -----------------------------------------------------------------------
    // CSV export – fetch all matching rows without pagination
    // -----------------------------------------------------------------------
    @Query("""
        SELECT u FROM User u
        WHERE (:search IS NULL
                OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.email)     LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR u.phone            LIKE CONCAT('%', CAST(:search AS string), '%'))
        AND (:role   IS NULL OR u.role   = :role)
        AND (:status IS NULL OR u.status = :status)
        AND (:school IS NULL OR LOWER(u.school) = LOWER(CAST(:school AS string)))
        AND (:region IS NULL OR LOWER(u.region) = LOWER(CAST(:region AS string)))
        ORDER BY u.createdAt DESC
        """)
    java.util.List<User> findAllForExport(
        @Param("search") String search,
        @Param("role")   Role role,
        @Param("status") UserStatus status,
        @Param("school") String school,
        @Param("region") String region
    );
}
