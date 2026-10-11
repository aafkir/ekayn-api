package com.ekayn.api.repo; import com.ekayn.api.model.OrderItem; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface OrderItemRepository extends JpaRepository<OrderItem,UUID> {}
