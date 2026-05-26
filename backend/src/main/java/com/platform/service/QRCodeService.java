package com.platform.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
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
        String recipientName = "БФАДД";
        String account = "40703810512000002359";
        String bank = "ВОЛОГОДСКОЕ ОТДЕЛЕНИЕ N8638 ПАО СБЕРБАНК г. Вологда";
        String bik = "041909644";
        String corrAccount = "30101810900000000644";
        String inn = "9715470089";
        String kpp = "771501001";
        String purpose = String.format("Подписка %s", paymentId);

        return String.format(
            "ST00012|Name=%s|PersonalAcc=%s|BankName=%s|BIC=%s|CorrespAcc=%s|Sum=%d|Purpose=%s|PayeeINN=%s|KPP=%s",
            recipientName, account, bank, bik, corrAccount,
            amount.multiply(new BigDecimal("100")).intValue(),
            purpose, inn, kpp
        );
    }
}