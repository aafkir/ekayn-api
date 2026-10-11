package com.ekayn.api.repo; import com.ekayn.api.model.User; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User,UUID> { Optional<User> findByEmailIgnoreCase(String email); Optional<User> findByVerificationTokenHash(String hash); Optional<User> findByResetTokenHash(String hash); }
