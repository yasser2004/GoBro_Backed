package com.gobro_backend.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.gobro_backend.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole_Name(String roleName, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<User> search(@Param("keyword") String keyword, Pageable pageable);

    long countByEnabledTrue();

    long countByRole_Name(String roleName);
}