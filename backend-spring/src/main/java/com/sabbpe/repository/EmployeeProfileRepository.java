package com.sabbpe.repository;

import com.sabbpe.model.EmployeeProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfileEntity, String> {

    Optional<EmployeeProfileEntity> findByUserId(String userId);

    Optional<EmployeeProfileEntity> findByEmail(String email);

    List<EmployeeProfileEntity> findByIsActiveTrue();

    boolean existsByEmail(String email);
}
