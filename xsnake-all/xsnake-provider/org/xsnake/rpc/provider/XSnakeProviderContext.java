package org.xsnake.rpc.provider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xsnake.rpc.provider.rmi.RMISupportHandler;
import org.xsnake.rpc.provider.rmq.RabbitMQSupportHandler;
import org.xsnake.rpc.provider.rmqrest.RestSupportHandler;

public class XSnakeProviderContext implements ApplicationContextAware {

	RegistryConfig registry = new RegistryConfig();
	
	RabbitMQSupportHandler rabbitMQSupportHandler = new RabbitMQSupportHandler();
	
	RestSupportHandler restSupportHandler = new RestSupportHandler();
	
	RMISupportHandler rmiSupportHandler = new RMISupportHandler();
	
	ApplicationContext applicationContext = null;
	
	String localAddress;
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		
		this.applicationContext = applicationContext;
		
		if (StringUtils.isEmpty(registry.application)) {
			throw new BeanCreationException("XSnake启动失败，配置参数registry.application不能为空");
		}

		//获取本机IP
		localAddress = getLocalHost();
		
		//读取远程配置
		/**
		try {
			remoteRegistryConfig.loadConfig(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(remoteRegistryConfig.isReady()){
			BeanUtils.copyProperties(remoteRegistryConfig, registry);
		}else{
			BeanUtils.copyProperties(registry, remoteRegistryConfig);
			try {
				remoteRegistryConfig.uploadConfig(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		**/
		
		//RMQ远程调用模式
		if(registry.rmqMode){
			rabbitMQSupportHandler.init(this);
		}
		
		//RMI远程调用模式
		if(registry.rmiMode){
			rmiSupportHandler.init(this);
		}
		
		if(registry.restMode){
			restSupportHandler.init(this);
		}
		
		System.out.println("=======初始化结束=======");
	}

	private String getLocalHost() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new BeanCreationException("创建对象失败，无法自动获取主机地址");
		}
	}

	public RegistryConfig getRegistry() {
		return registry;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public String getLocalAddress() {
		return localAddress;
	}
	
}
