package com.example.BookWhiz.util;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.nio.file.Path;

public class QrCodeGenerator {

    public static Path generateQRCode(String text, String filePath)
            throws Exception {

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text,
                BarcodeFormat.QR_CODE, 300, 300);

        Path path = Path.of(filePath);
        MatrixToImageWriter.writeToPath(matrix, "PNG", path);
        return path;
    }
}

