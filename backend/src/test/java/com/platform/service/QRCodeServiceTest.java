package com.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@ExtendWith(MockitoExtension.class)
class QRCodeServiceTest {

    @InjectMocks
    private QRCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(qrCodeService, "recipientName", "БФАДД");
        ReflectionTestUtils.setField(qrCodeService, "account", "40703810512000002359");
        ReflectionTestUtils.setField(qrCodeService, "bank", "ПАО Сбербанк");
        ReflectionTestUtils.setField(qrCodeService, "bik", "041909644");
        ReflectionTestUtils.setField(qrCodeService, "corrAccount", "30101810400000000225");
        ReflectionTestUtils.setField(qrCodeService, "inn", "9715470089");
        ReflectionTestUtils.setField(qrCodeService, "kpp", "771501001");
    }

    // ==================== generateQRCodeBase64 ====================
    @Test
    void generateQRCodeBase64_ValidData_ReturnsNonEmptyBase64() {
        String result = qrCodeService.generateQRCodeBase64("https://example.com");

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void generateQRCodeBase64_EmptyString_ThrowsException() {
        assertThatThrownBy(() -> qrCodeService.generateQRCodeBase64(""))
                .isInstanceOf(Exception.class);
    }

    @Test
    void generateQRCodeBase64_LongData_GeneratesSuccessfully() {
        String longData = "A".repeat(500);
        String result = qrCodeService.generateQRCodeBase64(longData);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    // ==================== generatePaymentQRData ====================
    @Test
    void generatePaymentQRData_ReturnsCorrectFormat() {
        BigDecimal amount = new BigDecimal("10000");
        String paymentId = "PAY-123-ABC";
        String username = "testuser";

        String result = qrCodeService.generatePaymentQRData(amount, paymentId, username);

        assertThat(result).startsWith("ST00012|");
        assertThat(result).contains("Name=БФАДД");
        assertThat(result).contains("PersonalAcc=40703810512000002359");
        assertThat(result).contains("BIC=041909644");
        assertThat(result).contains("Sum=1000000");
        assertThat(result).contains("Purpose=Подписка PAY-123-ABC");
        assertThat(result).contains("PayeeINN=9715470089");
        assertThat(result).contains("KPP=771501001");
    }

    @Test
    void generatePaymentQRData_SmallAmount_CalculatesCentsCorrectly() {
        BigDecimal amount = new BigDecimal("1.50");

        String result = qrCodeService.generatePaymentQRData(amount, "PAY-1", "user");

        assertThat(result).contains("Sum=150");
    }
}
