package com.ekayn.api.service;

import com.ekayn.api.api.ApiModels.*; import com.ekayn.api.model.*; import com.ekayn.api.repo.*; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.math.*; import java.time.*; import java.util.*;

@Service @RequiredArgsConstructor
public class OrderService {
  public static final BigDecimal FREE_SHIPPING_THRESHOLD=new BigDecimal("69.00");
  public static final BigDecimal STANDARD_HOME_DELIVERY_COST=new BigDecimal("6.90");
  public static final String STANDARD_HOME_DELIVERY_LABEL="Livraison à domicile avec suivi";
  private final OrderRepository orders; private final AddressRepository addresses; private final NotificationService notifications;

  @Transactional(readOnly=true) public ShippingQuoteResponse quote(User user,OrderInput input){
    ShippingData data=calculate(user,input); return new ShippingQuoteResponse(data.subtotal(),data.shippingCost(),data.subtotal().add(data.shippingCost()),"EUR",data.method().name(),data.label());
  }

  @Transactional public Order create(User user,OrderInput input,String idempotencyKey){
    ShippingData data=calculate(user,input); Address a=data.address(); Order o=new Order(); o.setUser(user); o.setCheckoutIdempotencyKey(idempotencyKey);
    o.setShippingName(user.getFirstName()+" "+user.getLastName()); o.setShippingLine1(a.getLine1()); o.setShippingLine2(a.getLine2()); o.setShippingCity(a.getCity()); o.setShippingPostalCode(a.getPostalCode()); o.setShippingCountry(a.getCountry()); o.setShippingAddressSnapshot(addressSnapshot(o));
    o.setShippingMethod(data.method()); o.setShippingMethodLabel(data.label()); o.setSubtotal(data.subtotal()); o.setShippingCost(data.shippingCost()); o.setTotal(data.subtotal().add(data.shippingCost()));
    for(ItemInput i:input.items()){Product p=Product.catalog().get(i.productId()); OrderItem x=new OrderItem(); x.setOrder(o); x.setProductId(i.productId()); x.setProductName(p.name()); x.setVariant(i.variant()); x.setQuantity(i.quantity()); x.setUnitPrice(p.price()); o.getItems().add(x);}
    return orders.save(o);
  }

  private ShippingData calculate(User user,OrderInput input){
    Address a=addresses.findByIdAndUserId(input.addressId(),user.getId()).orElseThrow(()->new IllegalArgumentException("Adresse introuvable."));
    String postal=a.getPostalCode()==null?"":a.getPostalCode().replaceAll("\\D","");
    if(!"FR".equalsIgnoreCase(a.getCountry())||postal.matches("^(97|98)\\d{3}$")) throw new IllegalArgumentException("La livraison est limitée à la France métropolitaine.");
    Enums.ShippingMethod method; try{method=Enums.ShippingMethod.valueOf(input.shippingMethod()==null?Enums.ShippingMethod.STANDARD_HOME_DELIVERY.name():input.shippingMethod());}catch(Exception e){throw new IllegalArgumentException("Mode de livraison invalide.");}
    BigDecimal subtotal=BigDecimal.ZERO; for(ItemInput i:input.items()){Product p=Product.catalog().get(i.productId());if(p==null)throw new IllegalArgumentException("Produit inconnu.");subtotal=subtotal.add(p.price().multiply(BigDecimal.valueOf(i.quantity())));}
    BigDecimal cost=subtotal.compareTo(FREE_SHIPPING_THRESHOLD)>=0?BigDecimal.ZERO:STANDARD_HOME_DELIVERY_COST;
    return new ShippingData(a,method,STANDARD_HOME_DELIVERY_LABEL,subtotal,cost);
  }

  @Transactional(readOnly=true) public List<OrderSummaryResponse> mine(User u){return orders.findByUserIdOrderByCreatedAtDesc(u.getId()).stream().map(this::summary).toList();}
  @Transactional(readOnly=true) public OrderDetailResponse mineOne(User u,UUID id){return orders.findByIdAndUserId(id,u.getId()).map(this::detail).orElseThrow(()->new NoSuchElementException("Commande introuvable."));}
  @Transactional(readOnly=true) public List<AdminOrderResponse> all(){return orders.findAllByOrderByCreatedAtDesc().stream().map(this::admin).toList();}
  @Transactional public AdminOrderResponse update(UUID id,DeliveryUpdate input){Order o=orders.findById(id).orElseThrow(()->new NoSuchElementException("Commande introuvable.")); Enums.DeliveryStatus next; try{next=Enums.DeliveryStatus.valueOf(input.deliveryStatus());}catch(Exception e){throw new IllegalArgumentException("Statut de livraison invalide.");} o.setDeliveryStatus(next);o.setCarrier(input.carrier());o.setTrackingNumber(input.trackingNumber());o.setTrackingUrl(input.trackingUrl()); if(input.shippedAt()!=null)o.setShippedAt(input.shippedAt()); if(next==Enums.DeliveryStatus.EXPEDIEE&&o.getShippedAt()==null)o.setShippedAt(Instant.now());o.setUpdatedAt(Instant.now()); if(next==Enums.DeliveryStatus.EXPEDIEE&&!o.isShippingNotificationSent()){o.setShippingNotificationSent(true);notifications.shipped(o);} return admin(orders.save(o));}
  public Order save(Order o){return orders.save(o);}
  public OrderSummaryResponse summary(Order o){return new OrderSummaryResponse(o.getId(),number(o),o.getCreatedAt(),o.getTotal(),o.getCurrency(),o.getPaymentStatus().name(),o.getDeliveryStatus().name(),items(o));}
  public OrderDetailResponse detail(Order o){return new OrderDetailResponse(o.getId(),number(o),o.getCreatedAt(),o.getSubtotal(),o.getShippingCost(),o.getTotal(),o.getCurrency(),o.getPaymentStatus().name(),o.getDeliveryStatus().name(),o.getShippingMethod().name(),o.getShippingMethodLabel(),o.getShippingAddressSnapshot(),o.getCarrier(),o.getTrackingNumber(),o.getTrackingUrl(),o.getShippedAt(),items(o),o.getShippingName(),o.getShippingLine1(),o.getShippingLine2(),o.getShippingCity(),o.getShippingPostalCode(),o.getShippingCountry());}
  public AdminOrderResponse admin(Order o){return new AdminOrderResponse(o.getId(),number(o),o.getCreatedAt(),o.getUser().getEmail(),o.getSubtotal(),o.getShippingCost(),o.getTotal(),o.getCurrency(),o.getPaymentStatus().name(),o.getDeliveryStatus().name(),o.getShippingMethod().name(),o.getShippingMethodLabel(),o.getShippingAddressSnapshot(),o.getCarrier(),o.getTrackingNumber(),o.getTrackingUrl(),o.getShippedAt(),items(o),o.getShippingName(),o.getShippingLine1(),o.getShippingLine2(),o.getShippingCity(),o.getShippingPostalCode(),o.getShippingCountry());}
  private List<OrderItemResponse> items(Order o){return o.getItems().stream().map(i->new OrderItemResponse(i.getProductId(),i.getProductName(),i.getVariant(),i.getQuantity(),i.getUnitPrice(),i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList();}
  private String number(Order o){return "EK-"+o.getCreatedAt().getEpochSecond()+"-"+o.getId().toString().substring(0,6).toUpperCase(Locale.ROOT);}
  private String addressSnapshot(Order o){return String.join("\n",o.getShippingName(),o.getShippingLine1(),o.getShippingLine2()==null?"":o.getShippingLine2(),o.getShippingPostalCode()+" "+o.getShippingCity(),o.getShippingCountry());}
  private record ShippingData(Address address,Enums.ShippingMethod method,String label,BigDecimal subtotal,BigDecimal shippingCost){}
}
record Product(String name,BigDecimal price){static Map<String,Product> catalog(){return Map.of("1",new Product("Éclat de tradition",new BigDecimal("19.90")),"2",new Product("Collier Louban",new BigDecimal("59.90")),"3",new Product("Lien d’héritage",new BigDecimal("24.90")),"4",new Product("Tazrzit amazighe",new BigDecimal("19.90")),"5",new Product("Spirales d’héritage",new BigDecimal("19.90")),"6",new Product("Médailles du Sud",new BigDecimal("19.90")));}}
