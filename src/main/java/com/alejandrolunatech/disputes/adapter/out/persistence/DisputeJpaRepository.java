package com.alejandrolunatech.disputes.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

interface DisputeJpaRepository extends JpaRepository<DisputeEntity, UUID> {

    Optional<DisputeEntity> findByIdempotencyKey(String idempotencyKey);

    List<DisputeEntity> findByStatusOrderByOpenedAtAsc(String status, Limit limit);
}
