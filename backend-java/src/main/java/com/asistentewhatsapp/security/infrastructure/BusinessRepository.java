package com.asistentewhatsapp.security.infrastructure;

import com.asistentewhatsapp.security.domain.BusinessEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<BusinessEntity, UUID> {

    List<BusinessEntity> findByActiveTrueOrderByCreatedAtAsc();

    Optional<BusinessEntity> findFirstByActiveTrueOrderByCreatedAtAsc();

    Optional<BusinessEntity> findByCode(String code);
}
