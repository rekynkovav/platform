package com.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column()
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "RUB";

    @Column(name = "subscription_months", nullable = false)
    private Integer subscriptionMonths;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_method")
    private String paymentMethod; // SBP, BANK_TRANSFER, CARD

    @Column(name = "bank_transaction_id")
    private String bankTransactionId; // ID транзакции из банка

    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData; // Данные для QR-кода СБП

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Срок действия платежа (24 часа)

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "confirmed_by")
    private String confirmedBy; // ID админа, который подтвердил вручную

    @Column(columnDefinition = "TEXT")
    private String notes; // Заметки админа

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public enum PaymentStatus {
        PENDING,      // Ожидает оплаты
        PAID,         // Оплачен
        CONFIRMED,    // Подтвержден (роль обновлена)
        EXPIRED,      // Истек срок
        CANCELLED,    // Отменен
        REFUNDED      // Возврат
    }
}
