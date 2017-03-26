package org.xsnake.rpc.provider.rmqrest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.xsnake.rpc.api.Remote;
import org.xsnake.rpc.api.RequestMethod;
import org.xsnake.rpc.api.Rest;
import org.xsnake.rpc.common.ReflectionUtil;
import org.xsnake.rpc.connector.RabbitMQConfig;
import org.xsnake.rpc.connector.RabbitMQConnector;
import org.xsnake.rpc.connector.RabbitMQWrapper;
import org.xsnake.rpc.provider.SupportHandler;
import org.xsnake.rpc.provider.XSnakeProviderContext;
import org.xsnake.rpc.rest.converter.BooleanConverter;
import org.xsnake.rpc.rest.converter.ConverterRegister;
import org.xsnake.rpc.rest.converter.DateConverter;
import org.xsnake.rpc.rest.converter.DoubleConverter;
import org.xsnake.rpc.rest.converter.FloatConverter;
import org.xsnake.rpc.rest.converter.IntegerConverter;
import org.xsnake.rpc.rest.converter.LongConverter;
import org.xsnake.rpc.rest.converter.ShortConverter;
import org.xsnake.rpc.rest.converter.StringConverter;
import org.xsnake.rpc.rmq.InvokeObject;
import org.xsnake.rpc.rmq.MessageBody;
import org.xsnake.rpc.rmq.MessageQueueWarpper;
import org.xsnake.rpc.rmq.RestRequestObject;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RestSupportHandler extends SupportHandler{

	Map<String, InvokeObject> invokeObjectMapping = new HashMap<String, InvokeObject>();
	
	MessageQueueWarpper queue = new MessageQueueWarpper();
	
	XSnakeProviderContext context;
	
	RabbitMQWrapper rabbitMQ = null;
	
	ConverterRegister converterRegister = new ConverterRegister();
	
	public void init(XSnakeProviderContext context) throws BeanCreationException{
		this.context = context;
		//连接RabbitMQ
		try {
			if(StringUtils.isEmpty(context.getRegistry().getMessageQueue())){
				throw new BeanCreationException("XSnake启动失败，RabbitMQ连接字符串不能为空。"); 
			}
			RabbitMQConfig rabbitMQConfig = RabbitMQConfig.getConfigure(context.getRegistry().getMessageQueue());
			rabbitMQ = new RabbitMQConnector(rabbitMQConfig);
		} catch (Exception e) {
			e.printStackTrace();
			throw new BeanCreationException("XSnake启动失败，无法连接到RabbitMQ服务器。" + e.getMessage());
		}
		
		export(context.getApplicationContext());
		try {
			listen(context);
		} catch (IOException e) {
			e.printStackTrace();
			throw new BeanCreationException("XSnake启动失败，无法连接到RabbitMQ服务器。" + e.getMessage());
		}
		createProcessThread();
		initConverterRegister();
	}
	
	/**
	 * 创建处理线程
	 */
	private void createProcessThread(){
		int maxThread = context.getRegistry().getMaxThread();
		for (int i = 0; i < maxThread; i++) {
			new RestThread(this).start();
		}
	}
	
	/**
	 * 查找符合条件的服务并导出
	 * @param applicationContext
	 */
	private void export(ApplicationContext applicationContext) {
		String[] names = applicationContext.getBeanDefinitionNames();
		for (String name : names) {
			Object obj = applicationContext.getBean(name);
			Object target = ReflectionUtil.getTarget(obj);
			Class<?>[] interfaces = target.getClass().getInterfaces();
			for (Class<?> interFace : interfaces) {
				Remote remote = interFace.getAnnotation(Remote.class);
				if (remote != null) {
					Method[] methods = interFace.getMethods();
					for (Method method : methods) {
						Rest rest = method.getAnnotation(Rest.class);
						if(rest !=null){
							RequestMethod[] httpMethods = rest.method();
							for(RequestMethod httpMethod : httpMethods){
								String key = RestRequestObject.createKey(httpMethod.toString(),rest.value());
								invokeObjectMapping.put(key, new InvokeObject(target, method));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 创建Channel，消费 ${ENVIRONMENT}_XSNAKE_INVOKE 队列来的请求。
	 * @param context
	 * @throws IOException
	 */
	private void listen(XSnakeProviderContext context) throws IOException{
		String queueName = context.getRegistry().getEnvironment() + "XSNAKE_REST";
		final Channel channel = rabbitMQ.getConnection().createChannel();
		try {
			channel.queueDeclare(queueName, false, false, false, null);
			channel.basicQos(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,byte[] body) throws IOException {
				queue.put(new MessageBody(channel, envelope, properties, body));
			}
		};
		try {
			channel.basicConsume(queueName, false, consumer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	AntPathMatcher antPathMatcher = new AntPathMatcher();
	
	protected InvokeObject findInvokeObject(String path,Map<String,String> dataMap){
		for(Map.Entry<String, InvokeObject> entry : invokeObjectMapping.entrySet()){
			if(antPathMatcher.match(entry.getKey(), path)){
				Map<String,String> map = antPathMatcher.extractUriTemplateVariables(entry.getKey(), path);
				dataMap.putAll(map);
				return entry.getValue();
			}
		}
		return null;
	}

	protected MessageQueueWarpper getQueue() {
		return queue;
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
	
}
