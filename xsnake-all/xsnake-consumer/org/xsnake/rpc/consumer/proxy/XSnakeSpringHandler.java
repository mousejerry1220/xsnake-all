package org.xsnake.rpc.consumer.proxy;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.xsnake.rpc.rest.RestPathService;
import org.xsnake.rpc.rest.RestServer;

public class XSnakeSpringHandler extends NamespaceHandlerSupport {

	public void init() {
		XSnakeBeanDefinitionParser parser = new XSnakeBeanDefinitionParser();
		registerBeanDefinitionParser("client", parser);
		RestServer r = new RestServer();
		r.run();
		RestPathService restPathService = new RestPathService(parser.targetList);
	}
	

}
