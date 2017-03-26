package org.xsnake.rpc.provider.rmqrest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.xsnake.rpc.common.MessageHandler;
import org.xsnake.rpc.rest.converter.ConverterException;
import org.xsnake.rpc.rest.converter.IConverter;
import org.xsnake.rpc.rmq.InvokeObject;
import org.xsnake.rpc.rmq.MessageBody;
import org.xsnake.rpc.rmq.ResponseObject;
import org.xsnake.rpc.rmq.RestRequestObject;

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
			String invokeKey = restRequestObject.toString();
			InvokeObject invokeObject = handler.findInvokeObject(invokeKey,restRequestObject.getParamters());
			if(invokeObject == null){
				throw new Exception("无效的访问路径!");
			}
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
				castValue(args, type, name, restRequestObject.getParamters());
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

	private void castValue(List<Object> args, Class<?> type, String name,Map<String, String> paramters) {
		String value = paramters.get(name);
		//查找注册的转换器
		IConverter<?> converter = handler.converterRegister.getConverter(type);
		if(converter != null){
			args.add(converter.converter(value));
			return;
		}
		
		//普通的java bean
		try {
			createParamterObject(type, paramters);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		//没有找到任何
		args.add(null);
			
	}

	private Object createParamterObject(Class<?> type, Map<String, String> paramters) throws InstantiationException, IllegalAccessException {
			Object obj = type.getClass().newInstance();
			for(Map.Entry<String, String> entry : paramters.entrySet()){
				String k = entry.getKey();
				String v = entry.getValue();
				try {
					Field field = type.getClass().getDeclaredField(k);
					field.setAccessible(true);
					Class<?> clazz = field.getType();
					IConverter<?> converter = handler.converterRegister.getConverter(clazz);
					if(converter !=null){
						Object r =converter.converter(v);
						field.set(obj, r);
					}
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
			}
		return null;
	}
	
}
