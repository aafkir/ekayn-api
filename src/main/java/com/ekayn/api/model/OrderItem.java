package com.ekayn.api.model;
import jakarta.persistence.*; import lombok.*; import java.math.*; import java.util.*; import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity @Getter @Setter @NoArgsConstructor
public class OrderItem {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @JsonIgnore @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private Order order;
  @Column(nullable=false) private String productId, productName; private String variant; @Column(nullable=false) private int quantity; @Column(nullable=false, precision=12, scale=2) private BigDecimal unitPrice;
}
