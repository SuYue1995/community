package com.yue.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class  CommunityApplication {

	@PostConstruct // 管理bean的生命周期、初始化 //被该注解修饰的方法在构造器调用完之后被执行，所以通常为初始化方法
	public void init() {
		// 解决Netty启动冲突的问题
		// 解决方法详见 Netty4Utils.setAvailableProcessors()
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}
	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}
}
