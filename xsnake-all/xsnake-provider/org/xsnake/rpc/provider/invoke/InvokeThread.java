package org.xsnake.rpc.provider.invoke;

import java.io.IOException;
import java.lang.reflect.Method;

import org.xsnake.rpc.common.InvokeRequestObject;
import org.xsnake.rpc.common.MessageHandler;
import org.xsnake.rpc.common.ResponseObject;
import org.xsnake.rpc.provider.InvokeObject;
import org.xsnake.rpc.provider.MessageBody;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public class InvokeThread extends Thread{

	InvokeSupportHandler handler;
	
	protected InvokeThread(InvokeSupportHandler handler){
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
		InvokeRequestObject invokeRequestObject = MessageHandler.bytesToObject(body);
		ResponseObject resultObject = new ResponseObject();
		try {
			String invokeKey = invokeRequestObject.toString();
			InvokeObject invokeObject = handler.findInvokeObject(invokeKey);
			Object target = invokeObject.getTarget();
			Method method = invokeObject.getMethod();
			method.setAccessible(true);
			Object result = method.invoke(target, invokeRequestObject.getArgs());
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
