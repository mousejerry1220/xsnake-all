package org.xsnake.rpc.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.Compression;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RestServer {
	
	public void run(){
		SpringApplication.run(RestServer.class);
	}
	
	@Bean
	public EmbeddedServletContainerFactory servletContainer(){
		AbstractEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
		factory.setPort(8094);
		Compression c = new Compression();
		c.setEnabled(true);
		c.setMimeTypes(new String[]{"text/json","text/html","text/xml","text/javascript","text/css","text/plain"});
		c.setMinResponseSize(256);
		factory.setCompression(c);
		return factory;
	}

}
