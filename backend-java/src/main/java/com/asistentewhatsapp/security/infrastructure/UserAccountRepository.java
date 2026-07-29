package com.asistentewhatsapp.security.infrastructure;

import com.asistentewhatsapp.security.domain.UserAccountEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

	Optional<UserAccountEntity> findByEmailIgnoreCase(String email);

	@Query("""
			select user
			from UserAccountEntity user
			where user.id = :userId
			  and user.businessId = :businessId
			""")
	Optional<UserAccountEntity> findScopedById(@Param("businessId") UUID businessId, @Param("userId") UUID userId);
}
