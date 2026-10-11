package com.ekayn.api.model;
import jakarta.persistence.*; import lombok.*; import java.util.*; import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity @Getter @Setter @NoArgsConstructor
public class Address {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @JsonIgnore @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private User user;
  @Column(nullable=false) private String label, line1, city, postalCode, country; private String line2; private boolean defaultAddress;
}
