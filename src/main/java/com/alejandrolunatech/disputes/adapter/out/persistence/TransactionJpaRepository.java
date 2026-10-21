package com.alejandrolunatech.disputes.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {
}
