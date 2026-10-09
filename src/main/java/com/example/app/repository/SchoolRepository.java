package com.example.app.repository;

import com.example.app.entity.School;
import com.example.app.enums.SchoolStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
    
    Optional<School> findBySchoolCode(String schoolCode);
    
    long countByStatus(SchoolStatus status);
    
    @Query("SELECT AVG(s.syncRate) FROM School s")
    Double getAverageSyncRate();

    @Query("SELECT s FROM School s WHERE " +
           "(:schoolName IS NULL OR LOWER(s.schoolName) LIKE LOWER(CONCAT('%', :schoolName, '%'))) AND " +
           "(:region IS NULL OR s.region = :region) AND " +
           "(:status IS NULL OR s.status = :status)")
    Page<School> searchSchools(
            @Param("schoolName") String schoolName, 
            @Param("region") String region, 
            @Param("status") SchoolStatus status, 
            Pageable pageable);

    @Query("SELECT s FROM School s WHERE " +
           "(:schoolName IS NULL OR LOWER(s.schoolName) LIKE LOWER(CONCAT('%', :schoolName, '%'))) AND " +
           "(:region IS NULL OR s.region = :region) AND " +
           "(:status IS NULL OR s.status = :status)")
    List<School> searchSchoolsForExport(
            @Param("schoolName") String schoolName, 
            @Param("region") String region, 
            @Param("status") SchoolStatus status);
}
