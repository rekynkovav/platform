package com.platform.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;

@Slf4j
@Service
public class QRCodeService {

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;

    @Value("${app.payment.recipient-name}")
    private String recipientName;

    @Value("${app.payment.account}")
    private String account;

    @Value("${app.payment.bank}")
    private String bank;

    @Value("${app.payment.bik}")
    private String bik;

    @Value("${app.payment.corr-account}")
    private String corrAccount;

    @Value("${app.payment.inn}")
    private String inn;

    @Value("${app.payment.kpp}")
    private String kpp;

    public String generateQRCodeBase64(String data) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public String generatePaymentQRData(BigDecimal amount, String paymentId, String username) {
        String purpose = String.format("Подписка %s", paymentId);

        return String.format(
            "ST00012|Name=%s|PersonalAcc=%s|BankName=%s|BIC=%s|CorrespAcc=%s|Sum=%d|Purpose=%s|PayeeINN=%s|KPP=%s",
            recipientName, account, bank, bik, corrAccount,
            amount.multiply(new BigDecimal("100")).intValue(),
            purpose, inn, kpp
        );
    }
}