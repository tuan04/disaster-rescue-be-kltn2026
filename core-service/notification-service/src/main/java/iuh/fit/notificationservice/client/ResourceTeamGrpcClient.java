package iuh.fit.notificationservice.client;


import io.grpc.StatusRuntimeException;
import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.GetNotificationRecipientsByTeamIdsRequest;
import iuh.fit.common.grpc.GetNotificationRecipientsByTeamIdsResponse;
import iuh.fit.common.grpc.ResourceTeamGrpcServiceGrpc;
import iuh.fit.common.grpc.TeamNotificationRecipients;
import iuh.fit.notificationservice.dtos.TeamRecipientsSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ResourceTeamGrpcClient {
    private final ResourceTeamGrpcServiceGrpc.ResourceTeamGrpcServiceBlockingStub resourceTeamGrpcStub;

    private static final long TIMEOUT_SECONDS = 2;


    public Map<UUID, TeamRecipientsSnapshot> findRecipientsByTeamIds(
            List<UUID> teamIds
    ) {
        if (teamIds == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Danh sách teamIds không được null"
            );
        }

        // Không có đội cần tra cứu thì không cần gọi mạng.
        if (teamIds.isEmpty()) {
            return Map.of();
        }

        // Loại teamId trùng, đồng thời giữ thứ tự xuất hiện ban đầu.
        Set<UUID> requestedTeamIds = new LinkedHashSet<>();

        for (UUID teamId : teamIds) {
            if (teamId == null) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "teamIds không được chứa phần tử null"
                );
            }

            requestedTeamIds.add(teamId);
        }

        GetNotificationRecipientsByTeamIdsRequest request =
                GetNotificationRecipientsByTeamIdsRequest.newBuilder()
                        .addAllTeamIds(
                                requestedTeamIds.stream()
                                        .map(UUID::toString)
                                        .toList()
                        )
                        .build();

        final GetNotificationRecipientsByTeamIdsResponse response;

        try {
            response = resourceTeamGrpcStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getNotificationRecipientsByTeamIds(request);

        } catch (StatusRuntimeException exception) {
            throw new BusinessException(
                    ErrorCode.BAD_GATEWAY,
                    "Không lấy được người nhận từ resource-service"
            );
        }

        // Kiểm tra toàn bộ phản hồi rồi mới trả cho lớp gọi phía trên.
        return validateAndMapResponse(requestedTeamIds, response);
    }


    private Map<UUID, TeamRecipientsSnapshot> validateAndMapResponse(
            Set<UUID> requestedTeamIds,
            GetNotificationRecipientsByTeamIdsResponse response
    ) {
        // Giữ kết quả riêng cho từng đội để bước sau cache theo teamId.
        Map<UUID, TeamRecipientsSnapshot> result = new LinkedHashMap<>();

        for (TeamNotificationRecipients team : response.getTeamsList()) {
            UUID teamId = parseResponseUuid(team.getTeamId(), "teamId");

            if (!requestedTeamIds.contains(teamId)) {
                throw invalidResponse("Phản hồi chứa đội không có trong yêu cầu");
            }

            if (result.containsKey(teamId)) {
                throw invalidResponse("Phản hồi chứa teamId bị lặp");
            }

            if (!team.getTeamExists() && team.getUserIdsCount() > 0) {
                throw invalidResponse(
                        "Đội không tồn tại nhưng phản hồi vẫn chứa userIds"
                );
            }

            Set<UUID> userIds = new LinkedHashSet<>();

            for (String rawUserId : team.getUserIdsList()) {
                userIds.add(parseResponseUuid(rawUserId, "userId"));
            }
            result.put(
                    teamId,
                    new TeamRecipientsSnapshot(
                            teamId,
                            team.getTeamExists(),
                            List.copyOf(userIds)
                    )
            );
        }

        // Server phải trả kết quả cho tất cả đội được yêu cầu.
        // Thiếu một đội không đồng nghĩa với đội đó không tồn tại.
        if (!result.keySet().equals(requestedTeamIds)) {
            throw invalidResponse(
                    "Phản hồi thiếu kết quả của một hoặc nhiều đội"
            );
        }

        // Ngăn lớp gọi sửa cấu trúc Map sau khi nhận kết quả.
        return Collections.unmodifiableMap(result);
    }


    private BusinessException invalidResponse(String message) {
        return new BusinessException(
                ErrorCode.BAD_GATEWAY,
                message
        );
    }




    private UUID parseResponseUuid(String rawValue, String fieldName) {
        // Protobuf string thường trả chuỗi rỗng khi trường không được gán.
        if (rawValue == null || rawValue.isBlank()) {
            throw invalidResponse(
                    "Phản hồi thiếu " + fieldName
            );
        }

        final UUID parsed;

        try {
            parsed = UUID.fromString(rawValue);
        } catch (IllegalArgumentException exception) {
            // Đây là lỗi dữ liệu phản hồi từ service khác,
            // không phải lỗi đầu vào của người dùng notification.
            throw invalidResponse(
                    "Phản hồi chứa " + fieldName + " không đúng định dạng UUID"
            );
        }

        // Kiểm tra dạng UUID đầy đủ; chấp nhận chữ hoa hoặc chữ thường.
        if (!parsed.toString().equalsIgnoreCase(rawValue)) {
            throw invalidResponse(
                    "Phản hồi chứa " + fieldName + " không đúng định dạng UUID"
            );
        }

        return parsed;
    }


}
