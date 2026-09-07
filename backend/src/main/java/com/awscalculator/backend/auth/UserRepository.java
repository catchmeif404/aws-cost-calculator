package com.awscalculator.backend.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

    List<User> findTop200ByOrderByCreatedAtDesc();

    List<User> findTop200ByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCaseOrderByCreatedAtDesc(
            String email, String nickname);
}
