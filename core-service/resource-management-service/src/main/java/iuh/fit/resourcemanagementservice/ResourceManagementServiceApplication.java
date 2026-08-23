package iuh.fit.resourcemanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"iuh.fit.common", "iuh.fit.resourcemanagementservice"})
public class ResourceManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceManagementServiceApplication.class, args);
    }

}
