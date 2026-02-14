package com.backend_microservices.auth_service.repository;

import com.backend_microservices.auth_service.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// this repo allows us to perform database operations without writing SQL :)
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // this custom method is made and spring generates SQL code for it automatically, magic :)
    Optional<Role> findByRoleName(String roleName);
}
