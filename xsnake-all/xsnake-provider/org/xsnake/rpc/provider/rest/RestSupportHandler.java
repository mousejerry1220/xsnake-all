package org.xsnake.rpc.provider.rest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.util.AntPathMatcher;
import org.xsnake.rpc.api.Remote;
import org.xsnake.rpc.api.RequestMethod;
import org.xsnake.rpc.api.Rest;
import org.xsnake.rpc.common.ReflectionUtil;
import org.xsnake.rpc.common.RestRequestObject;
import org.xsnake.rpc.provider.InvokeObject;
import org.xsnake.rpc.provider.MessageBody;
import org.xsnake.rpc.provider.MessageQueueWarpper;
import org.xsnake.rpc.provider.XSnakeProviderContext;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RestSupportHandler {

	Map<String, InvokeObject> invokeObjectMapping = new HashMap<String, InvokeObject>();
	
	MessageQueueWarpper queue = new MessageQueueWarpper();
	
	XSnakeProviderContext context;
	
	public RestSupportHandler(XSnakeProviderContext context) {
		this.context = context;
	}
	
	public void init() throws IOException{
		export(context.getApplicationContext());
		listen(context);
		createProcessThread();
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
		final Channel channel = context.getRabbitMQ().getConnection().createChannel();
		try {
			channel.queueDeclare(queueName, false, false, false, null);
			channel.basicQos(context.getRegistry().getLoadCapacity());
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
	
}
