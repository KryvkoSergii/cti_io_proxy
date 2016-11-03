package ua.com.smiddle.cti.io.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ua.com.smiddle.cti.io.proxy.core.TransportStack;

import java.util.concurrent.Executor;

/**
 * Created by srg on 14.09.16.
 */
//@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "ua.smiddle.cti.proxy.core")
@PropertySource("classpath:application.properties")
public class Application {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
    }

    @Bean(name = "TransportStack", destroyMethod = "destroy")
    @Scope(value = "prototype")
    public TransportStack createTransportStack() {
        return new TransportStack();
    }

    @Bean(name = "threadPoolSender")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("EventSenderThread-");
        executor.initialize();
        return executor;
    }

}
