package com.ekayn.api.model;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.util.UUID;
@Entity @Getter @Setter @NoArgsConstructor public class RevokedToken { @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(nullable=false,unique=true) private String tokenId; @Column(nullable=false) private Instant expiresAt; public RevokedToken(String tokenId, Instant expiresAt){this.tokenId=tokenId;this.expiresAt=expiresAt;} }
