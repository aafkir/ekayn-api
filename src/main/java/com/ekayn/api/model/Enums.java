package com.ekayn.api.model;
public final class Enums {
  private Enums() {}
  public enum Role { CUSTOMER, ADMIN }
  public enum PaymentStatus { UNPAID, PENDING, PAID, FAILED, REFUNDED, CANCELLED, EXPIRED, CHECKOUT_FAILED }
  public enum DeliveryStatus { CONFIRMED, PREPARING, EXPEDIEE, SHIPPED, DELIVERED, CANCELLED, REFUNDED }
  public enum ShippingMethod { STANDARD_HOME_DELIVERY }
}
