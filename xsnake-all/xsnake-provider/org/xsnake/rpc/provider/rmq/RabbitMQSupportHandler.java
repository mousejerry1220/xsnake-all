package org.xsnake.rpc.provider.rmq;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.xsnake.rpc.api.Remote;
import org.xsnake.rpc.common.ReflectionUtil;
import org.xsnake.rpc.connector.RabbitMQConfig;
import org.xsnake.rpc.connector.RabbitMQConnector;
import org.xsnake.rpc.connector.RabbitMQWrapper;
import org.xsnake.rpc.provider.SupportHandler;
import org.xsnake.rpc.provider.XSnakeProviderContext;
import org.xsnake.rpc.rmq.InvokeObject;
import org.xsnake.rpc.rmq.MessageBody;
import org.xsnake.rpc.rmq.MessageQueueWarpper;
import org.xsnake.rpc.rmq.RequestObject;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RabbitMQSupportHandler extends SupportHandler{

	Map<String, InvokeObject> invokeObjectMapping = new HashMap<String, InvokeObject>();
	
	MessageQueueWarpper queue = new MessageQueueWarpper();
	
	XSnakeProviderContext context;
	
	RabbitMQWrapper rabbitMQ = null;
	
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
			throw new BeanCreationException("XSnake启动失败，无法连接到RabbitMQ服务器。" + e.getMessage());
		}
		createProcessThread();
	}
	
	/**
	 * 创建处理线程
	 */
	private void createProcessThread(){
		int maxThread = context.getRegistry().getMaxThread();
		for (int i = 0; i < maxThread; i++) {
			new InvokeThread(this).start();
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
						String invokeKey = RequestObject.createKey(context.getRegistry().getEnvironment(),interFace, method.getName(),method.getParameterTypes());
						invokeObjectMapping.put(invokeKey, new InvokeObject(target, method));
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
		String queueName = context.getRegistry().getEnvironment() + "_XSNAKE_INVOKE";
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
	
	protected InvokeObject findInvokeObject(String key){
		return invokeObjectMapping.get(key);
	}

	protected MessageQueueWarpper getQueue() {
		return queue;
	}
	
}
