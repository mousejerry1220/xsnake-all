package org.xsnake.rpc.provider;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public class MessageBody {
	
	public MessageBody(final Channel channel, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
		
		this.channel = channel;
		
		this.envelope = envelope;
		
		this.properties = properties;
		
		this.body = body;
	}
	
	Channel channel;
	
	Envelope envelope;
	
	AMQP.BasicProperties properties;
	
	byte[] body;

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public Envelope getEnvelope() {
		return envelope;
	}

	public void setEnvelope(Envelope envelope) {
		this.envelope = envelope;
	}

	public AMQP.BasicProperties getProperties() {
		return properties;
	}

	public void setProperties(AMQP.BasicProperties properties) {
		this.properties = properties;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

}
