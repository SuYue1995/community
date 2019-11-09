package com.yue.community;


import com.yue.community.dao.AlphaDao;
import com.yue.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class) //启用CommunityApplication为配置类
public class CommunityApplicationTests implements ApplicationContextAware { //实现ApplicationContextAware该接口，得到Spring容器（IoC自动创建容器）

	private ApplicationContext applicationContext; // 使用成员变量记录Spring容器

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException { //参数ApplicationContext接口即为Spring容器
		this.applicationContext = applicationContext;
	}

	@Test
	public void testApplicationContext(){
		System.out.println(applicationContext);
		//从容器中获取自动装配的Bean
		AlphaDao alphaDao = applicationContext.getBean(AlphaDao.class);
		System.out.println(alphaDao.select());

		alphaDao = applicationContext.getBean("alphaHibernate", AlphaDao.class); //把得到的Object转换成AlphaDao
		System.out.println(alphaDao.select());
	}

	@Test
	public void testBeanManagement(){ //用于测试Bean的管理方式
		AlphaService alphaService = applicationContext.getBean(AlphaService.class); // 按照类型获取.为了方便没有写接口，直接获取该类
		System.out.println(alphaService);

		alphaService = applicationContext.getBean(AlphaService.class); // 按照类型获取.为了方便没有写接口，直接获取该类
		System.out.println(alphaService);
	}

	@Test
	public void testBeanConfig(){
		SimpleDateFormat simpleDateFormat = applicationContext.getBean(SimpleDateFormat.class);
		System.out.println(simpleDateFormat.format(new Date()));
	}

	@Autowired
	@Qualifier("alphaHibernate")
	private AlphaDao alphaDao; //Spring将AlphaDao注入给这个alphaDao属性，再直接使用该属性即可

	@Autowired
	private AlphaService alphaService;

	@Autowired
	private SimpleDateFormat simpleDateFormat;

	@Test
	public void testDI (){ //测试Dependency Injection依赖注入
		System.out.println(alphaDao); //直接打印成员变量，查看是否能取到AlphaDao这个Bean
		System.out.println(alphaService);
		System.out.println(simpleDateFormat);
	}
}
