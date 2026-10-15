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

        scheduler.setPoolSize(1);

        scheduler.setThreadNamePrefix("dispatch-social-outbox-");

        return scheduler;
    }
}
