package iuh.fit.resourcemanagementservice.config;

import iuh.fit.common.grpc.UserGrpcServiceGrpc;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

@Configuration
@ImportGrpcClients(
        target = "user-service",
        types = UserGrpcServiceGrpc.UserGrpcServiceBlockingStub.class
)
public class GrpcClientConfig {
}
