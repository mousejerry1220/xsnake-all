package org.xsnake.rpc.consumer.proxy;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class XSnakeSpringHandler extends NamespaceHandlerSupport {

	public void init() {
		
		XSnakeBeanDefinitionParser parser = new XSnakeBeanDefinitionParser();
		
		registerBeanDefinitionParser("client", parser);
	}

}
