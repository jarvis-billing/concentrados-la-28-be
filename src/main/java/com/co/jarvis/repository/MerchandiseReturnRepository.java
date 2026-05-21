package com.co.jarvis.repository;

import com.co.jarvis.entity.MerchandiseReturn;
import com.co.jarvis.enums.EReturnStatus;
import com.co.jarvis.enums.EReturnType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchandiseReturnRepository extends MongoRepository<MerchandiseReturn, String> {

    Optional<MerchandiseReturn> findByReturnNumber(String returnNumber);

    List<MerchandiseReturn> findByReturnTypeOrderByReturnDateDesc(EReturnType returnType);

    List<MerchandiseReturn> findByStatusOrderByReturnDateDesc(EReturnStatus status);

    List<MerchandiseReturn> findByClientIdOrderByReturnDateDesc(String clientId);

    List<MerchandiseReturn> findBySupplierIdOrderByReturnDateDesc(String supplierId);

    List<MerchandiseReturn> findByOriginalDocumentId(String originalDocumentId);

    List<MerchandiseReturn> findByReturnDateBetweenOrderByReturnDateDesc(OffsetDateTime from, OffsetDateTime to);

    MerchandiseReturn findFirstByOrderByCreatedAtDesc();
}
