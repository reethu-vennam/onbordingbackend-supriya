package com.sabbpe.repository;

import com.sabbpe.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserEntity u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<UserEntity> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u JOIN FETCH u.roles WHERE u.id = :id")
    Optional<UserEntity> findByIdWithRoles(@Param("id") String id);
}
