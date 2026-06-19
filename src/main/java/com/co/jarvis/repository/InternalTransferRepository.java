package com.co.jarvis.repository;

import com.co.jarvis.entity.InternalTransfer;
import com.co.jarvis.enums.EInternalTransferStatus;
import com.co.jarvis.enums.EInternalTransferType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InternalTransferRepository extends MongoRepository<InternalTransfer, String> {

    List<InternalTransfer> findByTransferDate(LocalDate transferDate);

    List<InternalTransfer> findByTransferDateAndStatus(LocalDate transferDate, EInternalTransferStatus status);

    List<InternalTransfer> findByTransferDateAndStatusAndDestinationBankAccountId(
            LocalDate transferDate, EInternalTransferStatus status, String destinationBankAccountId);

    List<InternalTransfer> findByTransferDateAndTypeAndStatus(
            LocalDate transferDate, EInternalTransferType type, EInternalTransferStatus status);

    // @Query overrides the derived $gt/$lt (exclusive) with $gte/$lte (inclusive)
    // so transfers registered on the boundary dates (e.g. today) are always included.
    @Query("{ 'transfer_date': { $gte: ?0, $lte: ?1 } }")
    List<InternalTransfer> findByTransferDateBetween(LocalDate from, LocalDate to);

    @Query("{ 'transfer_date': { $gte: ?0, $lte: ?1 }, 'status': ?2 }")
    List<InternalTransfer> findByTransferDateBetweenAndStatus(
            LocalDate from, LocalDate to, EInternalTransferStatus status);

    List<InternalTransfer> findByStatus(EInternalTransferStatus status);
}
