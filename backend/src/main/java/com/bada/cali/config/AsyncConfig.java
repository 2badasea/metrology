package com.bada.cali.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리(@Async) 설정
 *
 * 데모 모드 배치 처리(DemoJobProcessorService)가 @Async로 실행되므로 반드시 필요.
 * cali-worker 실제 연동 모드에서는 @Async를 사용하지 않지만,
 * 설정은 항상 활성화된 상태로 유지한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 동시 처리 가능한 데모 배치 수 (코어: 2, 최대: 5)
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        // 대기 큐 크기 (5건 이상 동시 요청 시 대기)
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("demo-job-");
        executor.initialize();
        return executor;
    }
}
