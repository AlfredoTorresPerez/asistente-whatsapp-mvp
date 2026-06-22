package com.asistentewhatsapp.security.infrastructure;

import com.asistentewhatsapp.security.domain.SecurityPolicyEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicyEntity, UUID> {

    Optional<SecurityPolicyEntity> findByBusinessId(UUID businessId);
}

