package com.asistentewhatsapp.security.infrastructure;

import com.asistentewhatsapp.security.domain.PasswordResetTokenEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

	Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

	List<PasswordResetTokenEntity> findAllByUserIdAndConsumedAtIsNullAndExpiresAtAfter(UUID userId,
			OffsetDateTime expiresAt);
}
