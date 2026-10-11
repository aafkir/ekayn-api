package com.ekayn.api.repo; import com.ekayn.api.model.RevokedToken; import org.springframework.data.jpa.repository.JpaRepository; import java.util.UUID;
public interface RevokedTokenRepository extends JpaRepository<RevokedToken,UUID> { boolean existsByTokenId(String tokenId); }
