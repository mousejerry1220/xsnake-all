package org.xsnake.rpc.consumer.rmi;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.xsnake.rpc.connector.ZooKeeperConnector;
import org.xsnake.rpc.connector.ZooKeeperWrapper;

public class XSnakeProxyFactory {

	
	String environment;
	
	ZooKeeperWrapper zooKeeper;
	
	public XSnakeProxyFactory(Map<String,String> propertyMap) throws Exception{
		
		environment = propertyMap.get("environment");
		
		String _zooKeeper = propertyMap.get("zooKeeper");
		
		int timeout = 10;
		try{
			timeout = propertyMap.get("initTimeout") == null ? 10 : Integer.parseInt(propertyMap.get("timeout"));
		}catch (NumberFormatException e){
			throw new BeanCreationException("XSnake启动失败，配置参数 initTimeout 错误，必须为数字类型，以秒计算");
		}

		if (StringUtils.isEmpty(_zooKeeper)) {
			throw new BeanCreationException("XSnake启动失败，配置参数 zooKeeper不能为空");
		}
		
		try {
			zooKeeper = new ZooKeeperConnector(_zooKeeper, timeout);
		} catch (Exception e) {
			throw new BeanCreationException("XSnake启动失败，无法连接到ZooKeeper服务器。" + e.getMessage());
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getService(Class<T> interfaceService){
		T handler = (T)new XSnakeProxyHandler(environment,zooKeeper,interfaceService).createProxy();
		return handler;
	}
	
	
	
}
