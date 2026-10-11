package com.ekayn.api.api;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
public final class ApiModels { private ApiModels(){}
 public record Register(@Email @NotBlank String email,@Size(min=8) String password,@NotBlank String firstName,@NotBlank String lastName){}
 public record Login(@Email @NotBlank String email,@NotBlank String password){}
 public record PasswordChange(@NotBlank String currentPassword,@Size(min=8) String newPassword,@NotBlank String confirmation){}
 public record PasswordReset(@Email @NotBlank String email,String token,@Size(min=8) String newPassword){}
 public record ForgotPassword(@Email @NotBlank String email){}
 public record Auth(@com.fasterxml.jackson.annotation.JsonIgnore String token, ProfileResponse user){}
 public record ProfileResponse(UUID id,String email,String firstName,String lastName,String phone,boolean emailVerified,String role){}
 public record AddressResponse(UUID id,String label,String line1,String line2,String city,String postalCode,String country,boolean defaultAddress){}
 public record OrderItemResponse(String productId,String productName,String variant,int quantity,BigDecimal unitPrice,BigDecimal lineTotal){}
 public record OrderSummaryResponse(UUID id,String orderNumber,Instant createdAt,BigDecimal total,String currency,String paymentStatus,String deliveryStatus,List<OrderItemResponse> items){}
 public record OrderDetailResponse(UUID id,String orderNumber,Instant createdAt,BigDecimal subtotal,BigDecimal shippingCost,BigDecimal total,String currency,String paymentStatus,String deliveryStatus,String shippingMethod,String shippingMethodLabel,String shippingAddressSnapshot,String carrier,String trackingNumber,String trackingUrl,Instant shippedAt,List<OrderItemResponse> items,String shippingName,String shippingLine1,String shippingLine2,String shippingCity,String shippingPostalCode,String shippingCountry){}
 public record AdminOrderResponse(UUID id,String orderNumber,Instant createdAt,String customerEmail,BigDecimal subtotal,BigDecimal shippingCost,BigDecimal total,String currency,String paymentStatus,String deliveryStatus,String shippingMethod,String shippingMethodLabel,String shippingAddressSnapshot,String carrier,String trackingNumber,String trackingUrl,Instant shippedAt,List<OrderItemResponse> items,String shippingName,String shippingLine1,String shippingLine2,String shippingCity,String shippingPostalCode,String shippingCountry){}
 public record Profile(String firstName,String lastName,String phone){}
 public record AddressInput(@NotBlank String label,@NotBlank String line1,String line2,@NotBlank String city,@NotBlank String postalCode,@NotBlank String country,boolean defaultAddress){}
 public record AccountDeletion(@NotBlank String currentPassword,@AssertTrue boolean confirmed){}
 public record ItemInput(@NotBlank String productId,String variant,@Positive int quantity){}
 public record OrderInput(@NotEmpty List<@Valid ItemInput> items,@NotNull UUID addressId,String shippingMethod){ public OrderInput(List<ItemInput> items,UUID addressId){this(items,addressId,null);} }
 public record ShippingQuoteResponse(BigDecimal subtotal,BigDecimal shippingCost,BigDecimal total,String currency,String shippingMethod,String shippingMethodLabel){}
 public record CheckoutSessionResponse(String checkoutUrl){}
 public record DeliveryUpdate(@NotBlank String deliveryStatus,String carrier,String trackingNumber,String trackingUrl,Instant shippedAt){}
}
