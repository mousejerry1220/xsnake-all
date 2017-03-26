package org.xsnake.rpc.consumer.rmq;

import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.StringUtils;
import org.xsnake.rpc.connector.RabbitMQConfig;
import org.xsnake.rpc.connector.RabbitMQConnector;
import org.xsnake.rpc.connector.RabbitMQWrapper;

public class XSnakeProxyFactory {

	RabbitMQWrapper rabbitMQ;
	
	String environment;
	
	public XSnakeProxyFactory(Map<String,String> propertyMap) throws Exception{
		String messageQueue = propertyMap.get("messageQueue");
		if(StringUtils.isEmpty(messageQueue)){
			throw new BeanCreationException("xsnake:property messageQueue 值不能为空");
		}
		environment = propertyMap.get("environment");
		RabbitMQConfig rabbitMQConfig = RabbitMQConfig.getConfigure(messageQueue);
		rabbitMQ = new RabbitMQConnector(rabbitMQConfig);
	}
	public RabbitMQWrapper getRabbitMQ() {
		return rabbitMQ;
	}
	
	public <T> T getService(Class<T> interfaceService){
		@SuppressWarnings("unchecked")
		T handler = (T)new XSnakeProxyHandler(environment,interfaceService).createProxy();
		return handler;
	}
	
	
	
}
