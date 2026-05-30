package com.platform.repository;

import com.platform.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByUserId(String userId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.expiresAt < :now")
    List<Payment> findExpiredPayments(LocalDateTime now);

    Optional<Payment> findByIdAndUserId(String id, String userId);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' ORDER BY p.createdAt DESC")
    List<Payment> findPendingPayments();

    List<Payment> findByUserIdAndStatus(String userId, Payment.PaymentStatus status);
}
