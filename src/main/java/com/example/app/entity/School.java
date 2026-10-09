package com.example.app.entity;

import com.example.app.enums.SchoolStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_name", nullable = false, length = 150)
    private String schoolName;

    @Column(name = "school_code", nullable = false, unique = true, length = 50)
    private String schoolCode;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SchoolStatus status = SchoolStatus.ACTIVE;

    @Column(name = "students_count")
    @Builder.Default
    private Integer studentsCount = 0;

    @Column(name = "teachers_count")
    @Builder.Default
    private Integer teachersCount = 0;

    @Column(name = "sync_rate")
    @Builder.Default
    private Double syncRate = 0.0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
