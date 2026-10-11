package com.ekayn.api.model;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.util.*;
@Entity @Table(name="users") @Getter @Setter @NoArgsConstructor
public class User {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
  @Column(nullable=false, unique=true) private String email; @Column(nullable=false) private String passwordHash;
  private String firstName, lastName, phone; private boolean emailVerified; private String verificationTokenHash, resetTokenHash; private Instant verificationTokenExpiresAt, resetTokenExpiresAt; private long sessionVersion; private Instant createdAt = Instant.now();
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Enums.Role role = Enums.Role.CUSTOMER;
  @OneToMany(mappedBy="user", cascade=CascadeType.ALL, orphanRemoval=true) private List<Address> addresses = new ArrayList<>();
}
