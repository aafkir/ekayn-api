package com.ekayn.api.model;
import jakarta.persistence.*; import lombok.*; import java.math.*; import java.time.*; import java.util.*; import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity @Table(name="orders") @Getter @Setter @NoArgsConstructor
public class Order {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @JsonIgnore @ManyToOne(fetch=FetchType.LAZY, optional=false) private User user;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Enums.PaymentStatus paymentStatus=Enums.PaymentStatus.UNPAID;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Enums.DeliveryStatus deliveryStatus=Enums.DeliveryStatus.CONFIRMED;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Enums.ShippingMethod shippingMethod;
  @Column(nullable=false) private String shippingMethodLabel;
  @Column(nullable=false, precision=12, scale=2) private BigDecimal subtotal=BigDecimal.ZERO, shippingCost=BigDecimal.ZERO, total; @Column(nullable=false) private String currency="EUR";
  private String stripePaymentIntentId, stripeCheckoutSessionId, stripeCheckoutUrl, checkoutIdempotencyKey;
  private String carrier, trackingNumber, trackingUrl; private Instant shippedAt; private boolean shippingNotificationSent, paymentNotificationSent;
  private Instant createdAt=Instant.now(), updatedAt=Instant.now();
  private String shippingName, shippingLine1, shippingLine2, shippingCity, shippingPostalCode, shippingCountry;
  @Column(columnDefinition="text") private String shippingAddressSnapshot;
  @OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval=true) private List<OrderItem> items=new ArrayList<>();
}
