package iuh.fit.userservice.service.impl;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000L;

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadAvatar(UUID userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw invalidImage("File ảnh không được rỗng");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw invalidImage("Ảnh đại diện tối đa 5 MB");
        }

        // Xác định định dạng từ nội dung và mã hóa lại ảnh.
        PreparedImage image = prepareImage(file);

        String key = "avatars/"
                + userId
                + "/"
                + UUID.randomUUID()
                + "."
                + image.extension();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(image.contentType())
                .build();

        try {
            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(image.bytes())
            );

            return s3Client.utilities()
                    .getUrl(builder -> builder
                            .bucket(bucketName)
                            .key(key))
                    .toExternalForm();
        } catch (SdkException exception) {
            throw new BusinessException(
                    ErrorCode.BAD_GATEWAY,
                    "Không thể tải ảnh lên, vui lòng thử lại"
            );
        }
    }

    private PreparedImage prepareImage(MultipartFile file) {
        try (var source = file.getInputStream();
             var input = ImageIO.createImageInputStream(source)) {

            if (input == null) {
                throw invalidImage("File ảnh không hợp lệ");
            }

            var readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw invalidImage("Chỉ hỗ trợ ảnh JPEG hoặc PNG");
            }

            var reader = readers.next();

            try {
                reader.setInput(input);

                String format = reader.getFormatName();

                boolean jpeg = "JPEG".equalsIgnoreCase(format);
                boolean png = "PNG".equalsIgnoreCase(format);

                if (!jpeg && !png) {
                    throw invalidImage("Chỉ hỗ trợ ảnh JPEG hoặc PNG");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0
                        || height <= 0
                        || (long) width * height > MAX_PIXELS) {
                    throw invalidImage(
                            "Ảnh không hợp lệ hoặc vượt quá 20 megapixel"
                    );
                }

                var decodedImage = reader.read(0);

                if (decodedImage == null) {
                    throw invalidImage("Không đọc được ảnh");
                }

                String extension = jpeg ? "jpg" : "png";
                String contentType = jpeg ? "image/jpeg" : "image/png";

                try (var output = new ByteArrayOutputStream()) {
                    boolean written = ImageIO.write(
                            decodedImage,
                            extension,
                            output
                    );

                    if (!written) {
                        throw invalidImage("Không thể xử lý ảnh");
                    }

                    if (output.size() > MAX_FILE_SIZE) {
                        throw invalidImage(
                                "Ảnh sau xử lý vượt quá 5 MB, "
                                        + "vui lòng chọn ảnh nhỏ hơn"
                        );
                    }

                    return new PreparedImage(
                            output.toByteArray(),
                            extension,
                            contentType
                    );
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw invalidImage("File ảnh bị lỗi hoặc không hợp lệ");
        }
    }

    private BusinessException invalidImage(String message) {
        return new BusinessException(
                ErrorCode.INVALID_INPUT,
                message
        );
    }

    private record PreparedImage(
            byte[] bytes,
            String extension,
            String contentType
    ) {
    }
}