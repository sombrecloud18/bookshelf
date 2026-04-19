package com.bookshelf.repository;

import com.bookshelf.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByLogin(String login);
    Optional<User> findByEmail(String email);
    boolean existsByLogin(String login);
    boolean existsByEmail(String email);
    List<User> findByRole(String role);
    Page<User> findAll(Pageable pageable);
    long countByRole(String role);
}
