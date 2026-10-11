package com.ekayn.api.model;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="stripe_events") @Getter @Setter @NoArgsConstructor public class StripeEvent { @Id private String eventId; @Column(nullable=false) private Instant processedAt=Instant.now(); public StripeEvent(String eventId){this.eventId=eventId;} }
