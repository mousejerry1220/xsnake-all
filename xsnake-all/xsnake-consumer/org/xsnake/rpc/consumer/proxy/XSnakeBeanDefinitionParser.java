package org.xsnake.rpc.consumer.proxy;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xsnake.rpc.api.Remote;
import org.xsnake.rpc.api.RequestMethod;
import org.xsnake.rpc.api.Rest;
import org.xsnake.rpc.consumer.rmi.XSnakeProxyFactory;
import org.xsnake.rpc.rest.RestRequestObject;
import org.xsnake.rpc.rest.TargetMethod;

public class XSnakeBeanDefinitionParser implements BeanDefinitionParser {

	Map<String, String> propertyMap = new HashMap<String, String>();

	Map<String, Class<?>> serviceMap = new HashMap<String, Class<?>>();
	
	Map<String, Method> restMap = new HashMap<String, Method>();
	
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		
		//获取所有的参数
		NodeList paroertyList = element.getElementsByTagName("xsnake:property");
		for (int i = 0; i < paroertyList.getLength(); i++) {
			Node node = paroertyList.item(i);
			Node keyNode = node.getAttributes().getNamedItem("name");
			Node valueNode = node.getAttributes().getNamedItem("value");
			if (keyNode == null) {
				throw new BeanCreationException("xsnake:property 必须包含 name 属性");
			}
			String key = keyNode.getNodeValue();
			String value = valueNode == null ? value = node.getTextContent() : valueNode.getNodeValue();
			if (value == null) {
				throw new BeanCreationException("xsnake:property " + key + " 没有设置值");
			}
			propertyMap.put(key, value.trim());
		}
		
		scanPacket(parserContext);
		
		// 设置连接环境
		String environment = StringUtils.isEmpty(propertyMap.get("environment")) ? "test" : propertyMap.get("environment");
		propertyMap.put("environment", environment);
		
		RootBeanDefinition clientBeanDefinition = new RootBeanDefinition();
		clientBeanDefinition.setBeanClass(XSnakeProxyFactory.class);
		ConstructorArgumentValues values = new ConstructorArgumentValues();
		values.addIndexedArgumentValue(0,propertyMap);
		clientBeanDefinition.setConstructorArgumentValues(values);
		parserContext.getRegistry().registerBeanDefinition("xsnakeProxy", clientBeanDefinition);

		for (Map.Entry<String, Class<?>> entry : serviceMap.entrySet()) {
			Class<?> interFace = entry.getValue();
			String serviceId = entry.getKey();
			RootBeanDefinition beanDefinition = new RootBeanDefinition();
			beanDefinition.setFactoryBeanName("xsnakeProxy");
			beanDefinition.setFactoryMethodName("getService");
			values = new ConstructorArgumentValues();
			values.addIndexedArgumentValue(0, interFace);
			beanDefinition.setConstructorArgumentValues(values);
			parserContext.getRegistry().registerBeanDefinition(serviceId, beanDefinition);
		}
		
		return null;
	}

	private void scanPacket(ParserContext parserContext) {
		String scanPackage = propertyMap.get("scanPackage");
		if(scanPackage == null){
			throw new BeanCreationException("没有指定要扫描的包位置");
		}
		String[] basePackages = scanPackage.split(";");
		
		for(String basePackage : basePackages){
			if(StringUtils.isEmpty(basePackage)){
				continue;
			}
			//扫描符合条件的接口
			ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
			String DEFAULT_RESOURCE_PATTERN = "**/*.class";
			MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
			String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + basePackage.replace('.', '/') + "/" + DEFAULT_RESOURCE_PATTERN;
			Resource[] resources = null;
			
			try {
				resources = resourcePatternResolver.getResources(packageSearchPath);
			} catch (IOException e) {
				e.printStackTrace();
				throw new BeanCreationException("包扫描失败:"+e.getMessage());
			}
			
			for (Resource resource : resources) {
				if (resource.isReadable()) {
					try {
						MetadataReader metadataReader =  metadataReaderFactory.getMetadataReader(resource);
						ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
						sbd.setResource(resource);
						sbd.setSource(resource);
						if(sbd.getMetadata().isInterface() && !sbd.getMetadata().isAnnotation()){
							String className = sbd.getMetadata().getClassName();
							Class<?> cls = Class.forName(className);
							Remote remote = cls.getAnnotation(Remote.class);
							if(remote!=null){
								String beanName = className.substring(className.lastIndexOf('.') + 1, className.length());
								System.out.println(beanName);
								serviceMap.put(beanName, cls);
								initRestService(parserContext,cls);
							}
						}
					}
					catch (Throwable ex) {
						throw new BeanDefinitionStoreException(
								"Failed to read candidate component class: " + resource, ex);
					}
				}
			}
		}
	}

	List<TargetMethod> targetList = new ArrayList<TargetMethod>();
	
	private void initRestService(ParserContext parserContext,Class<?> clazz) {
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			Rest rest = method.getAnnotation(Rest.class);
			if(rest !=null){
				RequestMethod[] httpMethods = rest.method();
				for(RequestMethod httpMethod : httpMethods){
					String restPath = RestRequestObject.createKey(httpMethod.toString(),rest.value());
					targetList.add(new TargetMethod(restPath, clazz, method));
				}
			}
		}
	}
	
}
