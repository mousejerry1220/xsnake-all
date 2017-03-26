package org.xsnake.rpc.provider.rmq;

import java.io.IOException;
import java.lang.reflect.Method;

import org.xsnake.rpc.common.MessageHandler;
import org.xsnake.rpc.rmq.InvokeObject;
import org.xsnake.rpc.rmq.MessageBody;
import org.xsnake.rpc.rmq.RequestObject;
import org.xsnake.rpc.rmq.ResponseObject;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public class InvokeThread extends Thread{

	RabbitMQSupportHandler handler;
	
	protected InvokeThread(RabbitMQSupportHandler handler){
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
	
	public void process(MessageBody message) throws IOException {
		final Channel channel = message.getChannel();
		Envelope envelope = message.getEnvelope();
		AMQP.BasicProperties properties = message.getProperties();
		byte[] body = message.getBody();
		AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().correlationId(properties.getCorrelationId()).build();
		RequestObject requestObject = MessageHandler.bytesToObject(body);
		ResponseObject resultObject = new ResponseObject();
		try {
			String invokeKey = requestObject.toString();
			InvokeObject invokeObject = handler.findInvokeObject(invokeKey);
			Object target = invokeObject.getTarget();
			Method method = invokeObject.getMethod();
			method.setAccessible(true);
			Object result = method.invoke(target, requestObject.getArgs());
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
