package iuh.fit.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    /**
     * Lỗi 500: Lỗi hệ thống máy chủ (Backend).
     * Ngữ cảnh: Xảy ra khi code bị lỗi logic (NullPointerException), mất kết nối
     * Database đột ngột, hoặc các ngoại lệ (Exception) chưa được catch.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "An unexpected error occurred"),

    /**
     * Lỗi 400: Dữ liệu đầu vào không hợp lệ.
     * Ngữ cảnh: Dùng khi người dùng gửi thiếu dữ liệu, sai định dạng JSON, hoặc
     * không vượt qua được các điều kiện kiểm tra (như @NotBlank, @Pattern, sai định
     * dạng email/số điện thoại).
     */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "400", "Invalid input data"),

    /**
     * Lỗi 404: Không tìm thấy tài nguyên.
     * Ngữ cảnh: Dùng khi truy vấn một dữ liệu không tồn tại trong DB (ví dụ: Tìm
     * User theo ID nhưng không có, hoặc gọi sai đường dẫn API URL).
     */
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "Resource not found"),

    /**
     * Lỗi 401: Lỗi chưa xác thực.
     * Ngữ cảnh: Dùng khi người dùng chưa đăng nhập, không truyền Token, hoặc Token
     * (JWT) đã bị hết hạn/không hợp lệ.
     */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "Unauthorized access"),

    /**
     * Lỗi 403: Lỗi cấm truy cập (Không đủ quyền).
     * Ngữ cảnh: Người dùng ĐÃ đăng nhập (có Token hợp lệ) nhưng KHÔNG CÓ QUYỀN thực
     * hiện hành động này. (Ví dụ: Citizen cố gắng gọi API xóa tài khoản chỉ dành
     * cho Admin).
     */
    FORBIDDEN(HttpStatus.FORBIDDEN, "403", "Access denied"),

    /**
     * Lỗi 409: Xung đột dữ liệu.
     * Ngữ cảnh: Dùng khi cố gắng tạo mới một dữ liệu mà nó đã tồn tại trong DB, vi
     * phạm tính duy nhất (Unique). (Ví dụ: Đăng ký bằng Số điện thoại hoặc Email đã
     * có người sử dụng).
     */
    CONFLICT(HttpStatus.CONFLICT, "409", "Resource already exists"),

    /**
     * Lỗi 502: Cổng kết nối tồi.
     * Ngữ cảnh: Thường gặp trong kiến trúc Microservices. API Gateway nhận được
     * request nhưng Service phía sau (như user-service) đang bị sập hoặc không phản
     * hồi.
     */
    BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "502", "Bad gateway");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
