package com.ekayn.api.api;

import com.ekayn.api.model.Enums;
import com.ekayn.api.model.Order;
import com.ekayn.api.repo.OrderRepository;
import com.ekayn.api.repo.StripeEventRepository;
import com.ekayn.api.service.NotificationService;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {
  private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
  private final OrderRepository orders;
  private final StripeEventRepository events;
  private final NotificationService notifications;
  @Value("${stripe.webhook-secret:}") String secret;

  @PostMapping("/webhook")
  @Transactional
  public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
    try {
      if (secret.isBlank() || signature == null) throw new IllegalArgumentException("Signature Stripe absente.");
      Event event = Webhook.constructEvent(payload, signature, secret);
      if (events.existsById(event.getId())) return ResponseEntity.ok().build();
      if (!java.util.Set.of("checkout.session.completed", "checkout.session.async_payment_succeeded", "checkout.session.async_payment_failed", "checkout.session.expired").contains(event.getType())) return ResponseEntity.ok().build();
      // Stripe CLI can deliver events created with a newer API version than the
      // stripe-java SDK. In that case the safe deserializer returns Optional.empty
      // even though the payload is valid; deserializeUnsafe keeps the raw fields.
      var deserializer = event.getDataObjectDeserializer();
      StripeObject object = deserializer.getObject().orElseGet(deserializer::deserializeUnsafe);
      if (!(object instanceof Session session)) throw new IllegalArgumentException("Objet Stripe inattendu.");
      Order order = orders.findByStripeCheckoutSessionIdForUpdate(session.getId()).orElseThrow(() -> new IllegalArgumentException("Session Stripe inconnue."));
      validateSession(order, session);
      try { events.saveAndFlush(new com.ekayn.api.model.StripeEvent(event.getId())); }
      catch (DataIntegrityViolationException duplicate) { return ResponseEntity.ok().build(); }

      var before = order.getPaymentStatus();
      switch (event.getType()) {
        case "checkout.session.completed" -> { if ("paid".equalsIgnoreCase(session.getPaymentStatus())) markPaid(order); }
        case "checkout.session.async_payment_succeeded" -> markPaid(order);
        case "checkout.session.async_payment_failed" -> markUnpaidFailure(order, Enums.PaymentStatus.FAILED);
        case "checkout.session.expired" -> markUnpaidFailure(order, Enums.PaymentStatus.EXPIRED);
        default -> { return ResponseEntity.ok().build(); }
      }
      order.setUpdatedAt(Instant.now());
      orders.save(order);
      if (before != Enums.PaymentStatus.PAID && order.getPaymentStatus() == Enums.PaymentStatus.PAID && !order.isPaymentNotificationSent()) {
        order.setPaymentNotificationSent(true);
        orders.save(order);
        try {
          notifications.orderPaid(order);
        } catch (Exception notificationError) {
          // A mail server outage must not make a successfully paid order fail.
          log.error("Paiement confirmé mais notification email impossible pour la commande {}", order.getId(), notificationError);
        }
      }
      return ResponseEntity.ok().build();
    } catch (Exception error) {
      log.error("Échec du traitement du webhook Stripe", error);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  private void validateSession(Order order, Session session) {
    if (!order.getId().toString().equals(session.getMetadata().get("order_id")) || !order.getId().toString().equals(session.getClientReferenceId())) throw new IllegalArgumentException("Métadonnées Stripe incohérentes.");
    long expectedCents = order.getTotal().movePointRight(2).longValueExact();
    if (session.getAmountTotal() == null || session.getAmountTotal() != expectedCents) throw new IllegalArgumentException("Montant Stripe incohérent.");
    if (!order.getCurrency().equalsIgnoreCase(session.getCurrency())) throw new IllegalArgumentException("Devise Stripe incohérente.");
  }

  private void markPaid(Order order) { order.setPaymentStatus(Enums.PaymentStatus.PAID); order.setDeliveryStatus(Enums.DeliveryStatus.CONFIRMED); }
  private void markUnpaidFailure(Order order, Enums.PaymentStatus status) { if (order.getPaymentStatus() != Enums.PaymentStatus.PAID) order.setPaymentStatus(status); }
}
