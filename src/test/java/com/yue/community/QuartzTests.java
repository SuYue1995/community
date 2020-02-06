package com.yue.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class QuartzTests {

    @Autowired
    private Scheduler scheduler;

    @Test
    public void testDeleteJob(){
        try {
            boolean result = scheduler.deleteJob(new JobKey("alphaJob", "alphaJobGroup")); // JobKey(job名，group名)
            System.out.println(result); // 打印结果，成功 or 失败
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
