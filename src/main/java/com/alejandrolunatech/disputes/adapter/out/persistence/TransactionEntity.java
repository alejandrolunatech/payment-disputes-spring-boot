package com.alejandrolunatech.disputes.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
class TransactionEntity {

    @Id
    String reference;

    @Column(nullable = false)
    BigDecimal amount;

    @Column(nullable = false)
    String currency;

    @Column(nullable = false)
    String merchant;

    @Column(name = "booked_on", nullable = false)
    LocalDate bookedOn;

    protected TransactionEntity() {
    }
}
