
package iuh.fit.integration.services;
import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.integration.entity.SocialApprovalPolicy;
import iuh.fit.integration.repository.SocialApprovalPolicyRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SocialApprovalPolicyService {

    private static final double DEFAULT_MIN_CONFIDENCE = 0.90;
    private final SocialApprovalPolicyRepository repository;
    private final Validator validator;

    public SocialApprovalPolicy getPolicy() {

        SocialApprovalPolicy policy = repository
                .findById(SocialApprovalPolicy.POLICY_ID)
                .orElseGet(this::createDefaultPolicy);

        validatePolicy(policy, ErrorCode.INTERNAL_SERVER_ERROR);

        // Document đã lưu phải có version để cập nhật an toàn.
        // Trường hợp này có thể xảy ra nếu policy được tạo thủ công
        // trước khi bổ sung field @Version.
        if (policy.version() == null) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Cấu hình tự duyệt đang thiếu trường version"
            );
        }

        // Trả về cấu hình hợp lệ cho caller.
        return policy;
    }


    private SocialApprovalPolicy createDefaultPolicy() {

        SocialApprovalPolicy defaultPolicy = new SocialApprovalPolicy(
                SocialApprovalPolicy.POLICY_ID,
                false,
                DEFAULT_MIN_CONFIDENCE,
                null,
                // Chưa insert nên chưa có version.
                // Spring Data sẽ khởi tạo khi insert.
                null
        );

        try {
            return repository.insert(defaultPolicy);
        } catch (DuplicateKeyException exception) {
            // Hai instance có thể cùng thấy chưa có cấu hình rồi cùng insert.
            // Một instance thắng; instance còn lại đọc document đã được tạo.
            return repository
                    .findById(SocialApprovalPolicy.POLICY_ID)
                    // Nếu vẫn không tìm thấy đúng document,
                    // giữ nguyên lỗi thay vì tự bỏ qua.
                    .orElseThrow(() -> exception);
        }
    }


    public SocialApprovalPolicy updatePolicy(
            Boolean enabled,
            Double minConfidence
    ) {

        SocialApprovalPolicy current = getPolicy();
        Instant enabledSince = current.enabledSince();

        // Chỉ đặt mốc mới khi chuyển từ tắt sang bật.
        // Boolean.TRUE.equals() tránh lỗi khi đầu vào enabled = null.
        if (Boolean.TRUE.equals(enabled)
                && !Boolean.TRUE.equals(current.enabled())) {

            // Ghi nhận thời điểm bắt đầu đợt tự duyệt mới.
            enabledSince = Instant.now();
        }

        // Record không có setter, nên tạo object chứa cấu hình mới.
        SocialApprovalPolicy updated = new SocialApprovalPolicy(
                SocialApprovalPolicy.POLICY_ID,
                enabled,
                minConfidence,
                enabledSince,
                current.version()
        );

        validatePolicy(updated, ErrorCode.INVALID_INPUT);

        try {
            return repository.save(updated);
        } catch (OptimisticLockingFailureException exception) {
            // Có thao tác khác cập nhật hoặc xóa document sau lúc ta đọc.
            // Không tự ghi đè; báo xung đột để caller đọc lại cấu hình.
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Cấu hình đã thay đổi. Hãy tải lại trước khi cập nhật"
            );
        }
    }


    private void validatePolicy(
            SocialApprovalPolicy policy,
            ErrorCode errorCode
    ) {

        var violations = validator.validate(policy);
        if (!violations.isEmpty()) {
            String details = violations.stream()

                    // Không đưa toàn bộ object cấu hình vào exception.
                    .map(violation ->
                            violation.getPropertyPath()
                                    + ": "
                                    + violation.getMessage()
                    )

                    // Giữ thứ tự thông báo ổn định.
                    .sorted()

                    // Ghép thành một chuỗi.
                    .collect(Collectors.joining("; "));

            // Caller quyết định đây là lỗi đầu vào hay dữ liệu hệ thống.
            throw new BusinessException(errorCode, details);
        }

        // Sau validation, minConfidence chắc chắn khác null.
        // Kiểm tra thêm để không chấp nhận NaN hoặc Infinity của Double.
        if (!Double.isFinite(policy.minConfidence())) {

            // Chỉ chấp nhận một giá trị số hữu hạn.
            throw new BusinessException(
                    errorCode,
                    "Ngưỡng confidence phải là số hữu hạn trong khoảng 0 đến 1"
            );
        }
    }
}