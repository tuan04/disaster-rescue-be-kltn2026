package iuh.fit.integration.services;


import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.social.SocialEvents;
import iuh.fit.integration.repository.SocialFeedStateRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class SocialDispatchResultService {
    private final SocialFeedStateRepository stateRepository;
    private final Validator validator;

    public void acceptCreated(SocialEvents.Created event) {

        // Validator.validate() không nhận đối tượng null.
        if (event == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Sự kiện kết quả không được để trống"
            );
        }

        var violations = validator.validate(event);

        // Dữ liệu không hợp lệ phải báo lỗi cho Kafka listener.
        if (!violations.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Sự kiện kết quả thiếu hoặc sai thông tin bắt buộc"
            );
        }

        // SOSResponse hiện chưa có @NotNull trên field id.
        // Vì vậy cần kiểm tra rõ ràng trước khi chuyển UUID thành String.
        if (event.rescue().id() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Kết quả dispatch thiếu ID yêu cầu cứu hộ"
            );
        }

        // Một thao tác MongoDB vừa kiểm tra điều kiện vừa cập nhật.
        boolean accepted = stateRepository.markDispatchCreated(event);

        // Không coi việc không khớp dữ liệu là xử lý thành công.
        // Không được âm thầm commit offset rồi bỏ mất kết quả.
        if (!accepted) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Kết quả dispatch không khớp feed, sự kiện phê duyệt "
                            + "hoặc yêu cầu cứu hộ đã lưu; externalId="
                            + event.externalId()
            );
        }

        // Không cần gửi Kafka hay thay đổi trạng thái outbox tại đây.
    }

}
