package iuh.fit.integration.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SocialSchedulingConfig {
    // Đặt tên riêng để các tác vụ social chọn đúng scheduler này.
    @Bean("socialTaskScheduler")
    public ThreadPoolTaskScheduler socialTaskScheduler() {

        // Tạo scheduler; Spring sẽ khởi tạo và quản lý vòng đời của nó.
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // Hai thread cho hai tác vụ:
        // một tác vụ xét duyệt và một tác vụ gửi outbox.
        scheduler.setPoolSize(2);

        // Đặt tiền tố tên thread để dễ nhận diện trong log.
        scheduler.setThreadNamePrefix("social-worker-");

        // Trả scheduler về cho Spring quản lý.
        // Không cần tự gọi initialize().
        return scheduler;
    }
}
