package iuh.fit.dispatchservice.config;

import iuh.fit.common.grpc.ResourceTeamGrpcServiceGrpc;
import iuh.fit.common.grpc.UserGrpcServiceGrpc;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

@Configuration
@ImportGrpcClients(
        target = "resource-management-service",
        types = ResourceTeamGrpcServiceGrpc.ResourceTeamGrpcServiceBlockingStub.class
)
@ImportGrpcClients(
        target = "user-service",
        types = UserGrpcServiceGrpc.UserGrpcServiceBlockingStub.class
)
public class GrpcClientConfig {
}
