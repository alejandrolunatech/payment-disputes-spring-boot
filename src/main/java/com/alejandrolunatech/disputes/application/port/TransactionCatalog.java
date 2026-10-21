package com.alejandrolunatech.disputes.application.port;

import com.alejandrolunatech.disputes.domain.Transaction;
import java.util.Optional;

/** Where transactions come from. In this project, a table seeded with made-up data. */
public interface TransactionCatalog {

    Optional<Transaction> findByReference(String reference);
}
