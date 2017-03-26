package org.xsnake.rpc.provider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xsnake.rpc.connector.RabbitMQConfig;
import org.xsnake.rpc.connector.RabbitMQConnector;
import org.xsnake.rpc.connector.RabbitMQWrapper;
import org.xsnake.rpc.connector.ZooKeeperConnector;
import org.xsnake.rpc.connector.ZooKeeperWrapper;
import org.xsnake.rpc.provider.invoke.InvokeSupportHandler;
import org.xsnake.rpc.provider.rest.RestSupportHandler;
import org.xsnake.rpc.rest.BooleanConverter;
import org.xsnake.rpc.rest.ConverterRegister;
import org.xsnake.rpc.rest.DateConverter;
import org.xsnake.rpc.rest.DoubleConverter;
import org.xsnake.rpc.rest.FloatConverter;
import org.xsnake.rpc.rest.IntegerConverter;
import org.xsnake.rpc.rest.LongConverter;
import org.xsnake.rpc.rest.ShortConverter;
import org.xsnake.rpc.rest.StringConverter;

public class XSnakeProviderContext implements ApplicationContextAware {

	RegistryConfig registry = new RegistryConfig();

	ZooKeeperWrapper zooKeeper = null;

	RabbitMQWrapper rabbitMQ = null;
	
	InvokeSupportHandler invokeSupportHandler=null;
	
	ApplicationContext applicationContext = null;
	
	ConverterRegister converterRegister = new ConverterRegister();
	
	RemoteRegistryConfig remoteRegistryConfig = new RemoteRegistryConfig();
	
	String localAddress;
	
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

		//获取本机IP
		localAddress = getLocalHost();
		
		//初始化XSNAKE主目录
		try {
			zooKeeper.dir("/"+registry.getEnvironment());
			zooKeeper.dir("/"+registry.getEnvironment()+"/XSNAKE");
			zooKeeper.dir("/"+registry.getEnvironment()+RemoteRegistryConfig.REMOTE_REGISTRY_CONFIG_PATH);
		} catch (Exception e) {
			throw new BeanCreationException("XSnake启动失败，初始化数据失败。" + e.getMessage());
		}
		
		//读取远程配置
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
		
		if(registry.restMode){
			RestSupportHandler restSupportHandler = new RestSupportHandler(this);
			try {
				restSupportHandler.init();
			} catch (IOException e) {
				e.printStackTrace();
				throw new BeanCreationException("XSnake启动失败，RabbitMQ在创建Channel时出现异常。" + e.getMessage());
			}
		}
		
		//加载转换器
		initConverterRegister();
		
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
	
	private void initConverterRegister() {
		converterRegister.register(String.class, new StringConverter());
		converterRegister.register(Date.class, new DateConverter());
		converterRegister.register(int.class, new IntegerConverter());
		converterRegister.register(Integer.class, new IntegerConverter());
		converterRegister.register(float.class, new FloatConverter());
		converterRegister.register(Float.class, new FloatConverter());
		converterRegister.register(Double.class, new DoubleConverter());
		converterRegister.register(double.class, new DoubleConverter());
		converterRegister.register(Long.class, new LongConverter());
		converterRegister.register(long.class, new LongConverter());
		converterRegister.register(Short.class, new ShortConverter());
		converterRegister.register(short.class, new ShortConverter());
		converterRegister.register(Boolean.class, new BooleanConverter());
		converterRegister.register(boolean.class, new BooleanConverter());
	}

	public void close() throws IOException, InterruptedException {
		rabbitMQ.close();
		zooKeeper.close();
	}

	public RegistryConfig getRegistry() {
		return registry;
	}

	public ZooKeeperWrapper getZooKeeper() {
		return zooKeeper;
	}

	public RabbitMQWrapper getRabbitMQ() {
		return rabbitMQ;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public ConverterRegister getConverterRegister() {
		return converterRegister;
	}

	public String getLocalAddress() {
		return localAddress;
	}
	
	
}
