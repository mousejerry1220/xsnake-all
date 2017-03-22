package org.xsnake.rpc.provider.invoke;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.xsnake.rpc.api.Remote;
import org.xsnake.rpc.common.InvokeRequestObject;
import org.xsnake.rpc.common.ReflectionUtil;
import org.xsnake.rpc.provider.InvokeObject;
import org.xsnake.rpc.provider.MessageBody;
import org.xsnake.rpc.provider.MessageQueueWarpper;
import org.xsnake.rpc.provider.XSnakeProviderContext;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class InvokeSupportHandler {

	Map<String, InvokeObject> invokeObjectMapping = new HashMap<String, InvokeObject>();
	
	MessageQueueWarpper queue = new MessageQueueWarpper();
	
	XSnakeProviderContext context;
	
	public InvokeSupportHandler(XSnakeProviderContext context) {
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
						String invokeKey = InvokeRequestObject.createKey(interFace, method.getName(),method.getParameterTypes());
						invokeObjectMapping.put(invokeKey, new InvokeObject(target, method));
					}
				}
			}
		}
	}

	/**
	 * 创建Channel，消费 XSNAKE_INVOKE_${APPLICATION} 队列来的请求。
	 * @param context
	 * @throws IOException
	 */
	private void listen(XSnakeProviderContext context) throws IOException{
		String queueName = "XSNAKE_INVOKE_"+context.getRegistry().getApplication();
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
	
	protected InvokeObject findInvokeObject(String key){
		return invokeObjectMapping.get(key);
	}

	protected MessageQueueWarpper getQueue() {
		return queue;
	}
	
}
