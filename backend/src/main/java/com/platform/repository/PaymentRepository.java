package com.platform.repository;

import com.platform.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByUserId(UUID userId, Pageable pageable);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.expiresAt < :now")
    List<Payment> findExpiredPayments(LocalDateTime now);

    Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' ORDER BY p.createdAt DESC")
    List<Payment> findPendingPayments(Pageable pageable);

    List<Payment> findByUserIdAndStatus(UUID userId, Payment.PaymentStatus status);
}
