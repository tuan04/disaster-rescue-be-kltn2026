package iuh.fit.userservice.config;


import iuh.fit.common.grpc.integration_serviceGrpc;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

@Configuration
@ImportGrpcClients(
        target = "integration-service",
        types = integration_serviceGrpc.integration_serviceBlockingStub.class
)
public class IntegrationGrpcClientConfig {

}
