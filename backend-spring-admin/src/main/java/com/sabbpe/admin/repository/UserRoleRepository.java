package com.sabbpe.admin.repository;

import com.sabbpe.admin.model.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, String> {
    List<UserRoleEntity> findByUserId(String userId);
    List<UserRoleEntity> findByRoleId(String roleId);
}
