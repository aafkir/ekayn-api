package com.ekayn.api.repo; import com.ekayn.api.model.StripeEvent; import org.springframework.data.jpa.repository.JpaRepository;
public interface StripeEventRepository extends JpaRepository<StripeEvent,String> {}
