package com.yue.community.config;

import com.yue.community.quartz.AlphaJob;
import com.yue.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

// 初始化：配置 -> 数据库
// 后续：数据库 -> 调用
@Configuration
public class QuartzConfig {

    // 整个IOC容器顶层接口 BeanFactory，区分于FactoryBean，两者完全不同
    // FactoryBean 可简化Bean的实例化过程：
    // 1. Spring通过FactoryBean封装Bean的实例化过程
    // 2. FactoryBean 装配到Spring容器里
    // 3. 将FactoryBean注入给其他的Bean
    // 4. 该Bean得到的是FactoryBean所管理的对象实例
    // 初始化JobDetailFactoryBean，装配到容器里，SimpleTriggerFactoryBean参数需要JobDetail，就把alphaJobDetail这个bean注入进来，
    // 默认得到的不是JobDetailFactoryBean，而是其中管理的对象JobDetail。这种方式初始化JobDetailFactoryBean比初始化JobDetail容易的多。

    // 配置JobDetail
//    @Bean
    public JobDetailFactoryBean alphaJobDetail(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class); // 声明管理Bean的类型
        factoryBean.setName("alphaJob"); // Job名字，不可重复
        factoryBean.setGroup("alphaJobGroup"); // 任务组名，多个任务可同属一组
        factoryBean.setDurability(true); // 声明任务持久保存，即使任务不再运行，也存着
        factoryBean.setRequestsRecovery(true); // 任务可恢复
        return factoryBean;
    }

    // 配置Trigger（两种选择：1. SimpleTriggerFactoryBean，简单trigger，e.g.每十分钟执行一次；
    // 2. CronTriggerFactoryBean，复杂trigger，e.g.每个月月底当天14点执行、每周周五12点执行）
//    @Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail){ // 初始化trigger依赖于JobDetail，注入参数
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail); // JobDetailFactoryBean/JobDetail 可以有多个实例，通过名字进行区分，两者之间需要同名对应
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000); // 重复间隔，每3s执行一次。
        factoryBean.setJobDataMap(new JobDataMap()); // Trigger底层存储job一些状态，可使用默认类型JobDataMap存储
        return factoryBean;
    }

    // 刷新帖子分数任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class); // 声明管理Bean的类型
        factoryBean.setName("postScoreRefreshJob"); // Job名字，不可重复
        factoryBean.setGroup("communityJobGroup"); // 任务组名，多个任务可同属一组
        factoryBean.setDurability(true); // 声明任务持久保存，即使任务不再运行，也存着
        factoryBean.setRequestsRecovery(true); // 任务可恢复
        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail){ // 初始化trigger依赖于JobDetail，注入参数
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail); // JobDetailFactoryBean/JobDetail 可以有多个实例，通过名字进行区分，两者之间需要同名对应
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5); // 重复间隔，每5m执行一次。
        factoryBean.setJobDataMap(new JobDataMap()); // Trigger底层存储job一些状态，可使用默认类型JobDataMap存储
        return factoryBean;
    }
}
