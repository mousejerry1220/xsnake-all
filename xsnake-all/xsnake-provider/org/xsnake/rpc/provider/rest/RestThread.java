package org.xsnake.rpc.provider.rest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.xsnake.rpc.common.MessageHandler;
import org.xsnake.rpc.common.ResponseObject;
import org.xsnake.rpc.common.RestRequestObject;
import org.xsnake.rpc.provider.InvokeObject;
import org.xsnake.rpc.provider.MessageBody;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public class RestThread extends Thread{

	RestSupportHandler handler;
	
	protected RestThread(RestSupportHandler handler){
		this.handler = handler;
	}
	
	@Override
	public void run() {
		while(true){
			MessageBody message = handler.getQueue().take();
			try {
				process(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
	public void process(MessageBody message) throws IOException {
		final Channel channel = message.getChannel();
		Envelope envelope = message.getEnvelope();
		AMQP.BasicProperties properties = message.getProperties();
		byte[] body = message.getBody();
		AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().correlationId(properties.getCorrelationId()).build();
		RestRequestObject restRequestObject = MessageHandler.bytesToObject(body);
		ResponseObject resultObject = new ResponseObject();
		try {
			Map<String,String> dataMap = new HashMap<String,String>();
			String invokeKey = restRequestObject.toString();
			InvokeObject invokeObject = handler.findInvokeObject(invokeKey,dataMap);
			if(invokeObject == null){
				throw new Exception("无效的访问路径!");
			}
			
			dataMap.putAll(restRequestObject.getParamters());
			
			//获取方法的参数名称
			Method targetMethod = invokeObject.getTarget().getClass().getDeclaredMethod(invokeObject.getMethod().getName(), invokeObject.getMethod().getParameterTypes());
			String[] parameterNames = parameterNameDiscoverer.getParameterNames(targetMethod);
			Class<?>[] parameterTypes = invokeObject.getMethod().getParameterTypes();
			
			//创建方法的访问参数
			//TODO 这里优化做验证器和转换器
			List<Object> args = new ArrayList<Object>();
			int parameterSize = parameterTypes.length;
			for(int i=0;i<parameterSize;i++){
				String name = parameterNames[i];
				Class<?> type = parameterTypes[i];
				String value = dataMap.get(name);
				if(type == String.class){
					args.add(value);
				}else if(type == int.class || type == Integer.class){
					args.add(Integer.valueOf(value));
				}else if(type == float.class || type == Float.class){
					args.add(Float.valueOf(value));
				}
				//TODO
			}

			//开始调用
			Object target = invokeObject.getTarget();
			Method method = invokeObject.getMethod();
			method.setAccessible(true);
			Object result = method.invoke(target, args.toArray());
			
			//返回结果
			resultObject.setResultObject(result);
			resultObject.setStatus(ResponseObject.SUCCESS);
		} catch (Exception e) {
			resultObject.setStatus(ResponseObject.ERROR);
			resultObject.setMessage(e.getMessage());
		} finally {
			channel.basicPublish("", properties.getReplyTo(), replyProps, MessageHandler.objectToBytes(resultObject));
			channel.basicAck(envelope.getDeliveryTag(), false);
		}
	}
}
