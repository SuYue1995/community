package com.yue.community;


import com.yue.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.concurrent.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ThreadPoolTests {

    // 实例化logger。在使用线程进行操作时，Logger在输出内容时会显示线程id和时间，适合多线程
    private Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);

    // JDK普通线程池，常用的两个.JDK的线程池都通过Executors工厂实例化。
    private ExecutorService executorService = Executors.newFixedThreadPool(5);//线程池初始化时包含5个线程，反复复用这五个创建好的线程

    // JDK可执行定时任务的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    // Spring框架初始化好线程池，并将其放在容器中进行管理，直接注入即可
    // Spring普通线程池
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    // Spring可执行定时任务线程池
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private AlphaService alphaService;

    //Junit测试和main方法不同。main方法中启动一个线程，如果不挂掉，main会等着该线程执行，不会立刻结束。
    // Junit中启动的子线程和当前线程是并发的，test方法中如果后面没有逻辑，不管线程有没有完成，都会结束。解决：让主线程sleep阻塞
    // sleep容易抛异常，简单进行封装
    private void sleep(long m){ // 当前线程阻塞m毫秒
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 1.JDK普通线程池
    @Test
    public void testExecutorService(){
        // 线程池需要任务执行，会分配一个线程来执行该任务。任务是一个线程体，通常实现Runable接口来提供线程体、任务。
        Runnable task = new Runnable() {
            @Override
            public void run() {
                // 执行的具体任务的逻辑
                logger.debug("Hello，ExecutorService");
            }
        };
        //使用线程池执行多次
        for (int i = 0; i < 10; i++) {
            // 调用submit方法，线程池分配线程来执行线程体
            executorService.submit(task);
        }
        sleep(10000);
    }

    // 2. JDK定时任务线程池
    @Test
    public void testScheduledExecutorService(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello, ScheduledExecutorService");
            }
        };
        // 以固定频率执行多次(任务，延迟时间10s，周期1s，时间单位ms)
        scheduledExecutorService.scheduleAtFixedRate(task, 10000, 1000, TimeUnit.MILLISECONDS);
        sleep(30000);
    }

    // 3. Spring普通线程池
    @Test
    public void testThreadPoolTaskExecutor(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello, ThreadPoolTaskExecutor");
            }
        };
        for (int i = 0; i < 10; i++) {
            taskExecutor.submit(task);
        }
        sleep(10000);
    }

    // 4. Spring定时任务线程池
    @Test
    public void testThreadPoolTaskScheduler(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello, ThreadPoolTaskScheduler");
            }
        };
        Date startTime = new Date(System.currentTimeMillis() + 10000);
        taskScheduler.scheduleAtFixedRate(task, startTime, 1000); // 默认为ms
        sleep(30000);
    }

    // 5. Spring普通线程池（简化方式）
    @Test
    public void testThreadPoolTaskExecutorSimple(){
        for (int i = 0; i <10 ; i++) {
            alphaService.execute1(); // 程序底层，Spring以多线程方式调用该方法。作为线程体，用线程池调用
        }
        sleep(10000);
    }

    // 6. Spring定时任务线程池(简化方式)
    @Test
    public void testThreadPoolTaskSchedulerSimple(){
        // 只要有程序在运行，定时任务就会被自动调用
        sleep(30000);
    }

}
