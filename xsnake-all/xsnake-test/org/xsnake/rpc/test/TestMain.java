package org.xsnake.rpc.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class TestMain {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		@SuppressWarnings("unused")
		ApplicationContext ctx = new FileSystemXmlApplicationContext("classpath:application-context.xml");
	}
}
