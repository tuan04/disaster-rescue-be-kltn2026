package iuh.fit.dispatchservice.config;

import iuh.fit.common.grpc.ResourceTeamGrpcServiceGrpc;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

@Configuration
@ImportGrpcClients(target = "resource-management-service", types = ResourceTeamGrpcServiceGrpc.ResourceTeamGrpcServiceBlockingStub.class)
public class GrpcClientConfig {
}
