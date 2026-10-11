package com.ekayn.api.repo; import com.ekayn.api.model.Address; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface AddressRepository extends JpaRepository<Address,UUID> { List<Address> findByUserId(UUID userId); Optional<Address> findByIdAndUserId(UUID id, UUID userId); }
