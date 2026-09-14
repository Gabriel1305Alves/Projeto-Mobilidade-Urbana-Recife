package br.com.embarcai.config;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QrCodeGenerator {
    @Value("${app.base-url:}")
    private String baseUrl;

    public byte[] png(String codigo, String requestUrl) throws Exception {
        String origem = (baseUrl == null || baseUrl.isBlank())
            ? requestUrl.replaceAll("/api/linhas/.*", "")
            : baseUrl.replaceAll("/$", "");
        String url = origem + "/linha/" + codigo;
        var hints = Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H, EncodeHintType.MARGIN, 1);
        var matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 640, 640, hints);
        var out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out, new MatrixToImageConfig(0xFF1B2430, 0xFFFFFFFF));
        return out.toByteArray();
    }
}
