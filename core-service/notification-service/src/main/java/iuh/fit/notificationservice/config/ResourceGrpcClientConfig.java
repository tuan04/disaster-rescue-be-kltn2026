package iuh.fit.notificationservice.config;

import iuh.fit.common.grpc.ResourceTeamGrpcServiceGrpc;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

@Configuration
@ImportGrpcClients(
        // Sử dụng channel đã khai báo trong notification-service.yml.
        target = "resource-management-service",

        // Đăng ký blocking stub được sinh từ resource_service.proto.
        // Blocking nghĩa là lời gọi sẽ chờ phản hồi hoặc kết thúc bằng lỗi.
        types = ResourceTeamGrpcServiceGrpc.ResourceTeamGrpcServiceBlockingStub.class
)
public class ResourceGrpcClientConfig {
    // Spring tự tạo bean stub dựa trên annotation phía trên.
}