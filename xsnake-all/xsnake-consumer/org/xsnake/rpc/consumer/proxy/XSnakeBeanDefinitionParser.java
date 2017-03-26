package org.xsnake.rpc.consumer.proxy;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XSnakeBeanDefinitionParser implements BeanDefinitionParser{

	Map<String,String> propertyMap = new HashMap<String,String>();
	
	Map<String,String> serviceMap = new HashMap<String,String>();
	
	public BeanDefinition parse(Element element, ParserContext parserContext) {  
		
		NodeList paroertyList = element.getElementsByTagName("xsnake:property");
		
		for(int i=0;i<paroertyList.getLength();i++){
			Node node = paroertyList.item(i);
			Node keyNode = node.getAttributes().getNamedItem("name");
			Node valueNode = node.getAttributes().getNamedItem("value");
			if(keyNode == null){
				throw new BeanCreationException("xsnake:property 必须包含 name 属性");
			}
			String key = keyNode.getNodeValue();
			String value = valueNode == null ? value = node.getTextContent() : valueNode.getNodeValue();
			if(value == null){
				throw new BeanCreationException("xsnake:property " + key + " 没有设置值");
			}
			propertyMap.put(key, value);
		}
		
		NodeList serviceList = element.getElementsByTagName("xsnake:service");
		for(int i=0;i<serviceList.getLength();i++){
			Node node = serviceList.item(i);
			String id = node.getAttributes().getNamedItem("id").getNodeValue();
			String interfaceName = node.getAttributes().getNamedItem("interface").getNodeValue();
			serviceMap.put(id, interfaceName);
		}
//        RootBeanDefinition clientBeanDefinition = new RootBeanDefinition();
//        clientBeanDefinition.setBeanClass(ClientAccessFactory.class);
//        clientBeanDefinition.getPropertyValues().add("url", url);
//        parserContext.getRegistry().registerBeanDefinition(id, clientBeanDefinition);
//        NodeList list = element.getChildNodes();
//        for(int i=0;i<list.getLength() ;i++){
//        	Node node = list.item(i);
//        	if("xsnake:service".equalsIgnoreCase(node.getNodeName())){
//        		String interfaceClass = node.getAttributes().getNamedItem("interface").getNodeValue();
//        		String serviceId = node.getAttributes().getNamedItem("id").getNodeValue();
//
//        		if(StringUtils.isEmpty(interfaceClass)){
//        			throw new IllegalArgumentException("xsnake:service node 'interface' attribute must be not null ");
//        		}
//        		
//        		if(StringUtils.isEmpty(serviceId)){
//        			throw new IllegalArgumentException("xsnake:service node 'id' attribute must be not null ");
//        		}
//        		
//        		RootBeanDefinition beanDefinition = new RootBeanDefinition();
//                beanDefinition.setFactoryMethodName("getService");
//                beanDefinition.setFactoryBeanName(id);
//                ConstructorArgumentValues values = new ConstructorArgumentValues();
//                try {
//					values.addIndexedArgumentValue(0, Class.forName(interfaceClass));
//				} catch (ClassNotFoundException e) {
//					e.printStackTrace();
//					throw new IllegalArgumentException(" class not found ! [" + interfaceClass + "]");
//				}
//                beanDefinition.setConstructorArgumentValues(values);
//                parserContext.getRegistry().registerBeanDefinition(serviceId, beanDefinition);
//        	}
//        }
//        return clientBeanDefinition;
		return null;
	}
	
}
