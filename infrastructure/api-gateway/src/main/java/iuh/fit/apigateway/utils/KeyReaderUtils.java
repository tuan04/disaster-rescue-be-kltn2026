package iuh.fit.apigateway.utils;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyReaderUtils {
    /**
     * Chuyển đổi chuỗi String (từ file private_java.pem) thành Object PrivateKey của Java
     */
    public static PrivateKey getPrivateKeyFromString(String key) throws Exception {
        // 1. Dọn dẹp chuỗi: Xóa header, footer và các ký tự xuống dòng/khoảng trắng
        String privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", ""); // Xóa mọi khoảng trắng và ký tự xuống dòng

        // 2. Giải mã chuỗi Base64 thành mảng byte
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        // 3. Dùng PKCS8 (chuẩn dành riêng cho Private Key) để tái tạo khóa
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Khai báo thuật toán RSA

        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Chuyển đổi chuỗi String (từ file public.pem) thành Object PublicKey của Java
     */
    public static PublicKey getPublicKeyFromString(String key) throws Exception {
        // 1. Dọn dẹp chuỗi: Xóa header, footer và các ký tự xuống dòng/khoảng trắng
        String publicKeyPEM = key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        // 2. Giải mã chuỗi Base64 thành mảng byte
        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

        // 3. Dùng X509 (chuẩn dành riêng cho Public Key) để tái tạo khóa
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }
}
