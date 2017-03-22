package org.xsnake.rpc.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.xsnake.rpc.connector.RabbitMQConfig;
import org.xsnake.rpc.connector.RabbitMQConnector;
import org.xsnake.rpc.connector.RabbitMQWrapper;
import org.xsnake.rpc.connector.ZooKeeperConnector;
import org.xsnake.rpc.connector.ZooKeeperWrapper;
import org.xsnake.rpc.provider.invoke.InvokeSupportHandler;

public class XSnakeProviderContext implements ApplicationContextAware {

	RegistryConfig registry = new RegistryConfig();

	ZooKeeperWrapper zooKeeper = null;

	RabbitMQWrapper rabbitMQ = null;
	
	InvokeSupportHandler invokeSupportHandler=null;
	
	ApplicationContext applicationContext = null;
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		
		this.applicationContext = applicationContext;
		
		if (StringUtils.isEmpty(registry.application)) {
			throw new BeanCreationException("XSnake启动失败，配置参数registry.application不能为空");
		}

		if (StringUtils.isEmpty(registry.zooKeeper)) {
			throw new BeanCreationException("XSnake启动失败，配置参数registry.zooKeeper不能为空");
		}

		//连接ZooKeeper
		try {
			zooKeeper = new ZooKeeperConnector(registry.zooKeeper, registry.timeout);
		} catch (IOException e) {
			e.printStackTrace();
			throw new BeanCreationException("XSnake启动失败，无法连接到ZooKeeper服务器。" + e.getMessage());
		} catch (TimeoutException e) {
			throw new BeanCreationException("XSnake启动失败，无法连接到ZooKeeper服务器。" + e.getMessage());
		}

		//连接RabbitMQ
		try {
			RabbitMQConfig rabbitMQConfig = RabbitMQConfig.getConfigure(registry.messageQueue);
			rabbitMQ = new RabbitMQConnector(rabbitMQConfig);
		} catch (Exception e) {
			e.printStackTrace();
			throw new BeanCreationException("XSnake启动失败，无法连接到RabbitMQ服务器。" + e.getMessage());
		}
		
		//JAVA远程调用模式
		if(registry.invokeMode){
			invokeSupportHandler = new InvokeSupportHandler(this);
			try {
				invokeSupportHandler.init();
			} catch (IOException e) {
				e.printStackTrace();
				throw new BeanCreationException("XSnake启动失败，RabbitMQ在创建Channel时出现异常。" + e.getMessage());
			}
		}
		
		System.out.println("=======初始化结束=======");
	}

	public void close() throws IOException, InterruptedException {
		rabbitMQ.close();
		zooKeeper.close();
	}

	public RegistryConfig getRegistry() {
		return registry;
	}

	public void setRegistry(RegistryConfig registry) {
		this.registry = registry;
	}

	public ZooKeeperWrapper getZooKeeper() {
		return zooKeeper;
	}

	public void setZooKeeper(ZooKeeperWrapper zooKeeper) {
		this.zooKeeper = zooKeeper;
	}

	public RabbitMQWrapper getRabbitMQ() {
		return rabbitMQ;
	}

	public void setRabbitMQ(RabbitMQWrapper rabbitMQ) {
		this.rabbitMQ = rabbitMQ;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
}
