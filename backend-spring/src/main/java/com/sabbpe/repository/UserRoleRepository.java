package com.sabbpe.repository;

import com.sabbpe.model.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, String> {

    List<UserRoleEntity> findByUserId(String userId);

    boolean existsByUserIdAndRoleId(String userId, String roleId);
}
