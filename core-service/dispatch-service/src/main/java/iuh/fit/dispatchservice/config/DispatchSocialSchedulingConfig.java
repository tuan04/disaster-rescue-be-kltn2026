package iuh.fit.dispatchservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class DispatchSocialSchedulingConfig {
    @Bean("dispatchSocialTaskScheduler")
    public ThreadPoolTaskScheduler dispatchSocialTaskScheduler() {

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // Hiện tại chỉ có một tác vụ gửi outbox nên dùng một thread.
        scheduler.setPoolSize(1);

        // Giúp nhận diện thread của tác vụ này trong log.
        scheduler.setThreadNamePrefix("dispatch-social-outbox-");

        // Spring tự khởi tạo scheduler sau khi đăng ký bean.
        // Không cần gọi initialize() thủ công.
        return scheduler;
    }
}
