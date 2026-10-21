package com.alejandrolunatech.disputes.adapter.out.persistence;

import com.alejandrolunatech.disputes.application.port.TransactionCatalog;
import com.alejandrolunatech.disputes.domain.Money;
import com.alejandrolunatech.disputes.domain.Transaction;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaTransactionCatalog implements TransactionCatalog {

    private final TransactionJpaRepository transactions;

    JpaTransactionCatalog(TransactionJpaRepository transactions) {
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findByReference(String reference) {
        return transactions.findById(reference).map(JpaTransactionCatalog::toDomain);
    }

    static Transaction toDomain(TransactionEntity entity) {
        return new Transaction(entity.reference, new Money(entity.amount, entity.currency), entity.merchant,
                entity.bookedOn);
    }
}
