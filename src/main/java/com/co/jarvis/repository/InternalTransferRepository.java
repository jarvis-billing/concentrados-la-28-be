package com.co.jarvis.repository;

import com.co.jarvis.entity.InternalTransfer;
import com.co.jarvis.enums.EInternalTransferStatus;
import com.co.jarvis.enums.EInternalTransferType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InternalTransferRepository extends MongoRepository<InternalTransfer, String> {

    List<InternalTransfer> findByTransferDate(LocalDate transferDate);

    List<InternalTransfer> findByTransferDateAndStatus(LocalDate transferDate, EInternalTransferStatus status);

    List<InternalTransfer> findByTransferDateAndTypeAndStatus(
            LocalDate transferDate, EInternalTransferType type, EInternalTransferStatus status);

    List<InternalTransfer> findByTransferDateBetween(LocalDate from, LocalDate to);

    List<InternalTransfer> findByTransferDateBetweenAndStatus(
            LocalDate from, LocalDate to, EInternalTransferStatus status);

    List<InternalTransfer> findByStatus(EInternalTransferStatus status);
}
