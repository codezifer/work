package com.bmw.bigdata.tracenet;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import io.swagger.jaxrs.config.BeanConfig;

public class ServiceConfig extends Application {

	public ServiceConfig() {
		// configureSwagger();
	}

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> resources = new HashSet<>();
		// resources.add(ApiListingResource.class);
		// resources.add(SwaggerSerializers.class);
		return resources;
	}

	private void configureSwagger() {
		BeanConfig beanConfig = new BeanConfig();
		beanConfig.setBasePath("/api");
		beanConfig.setTitle("Feger Service Search API");
		beanConfig.setVersion("1.0.0");
		beanConfig.setResourcePackage(FegerServiceSearch.class.getPackage().getName());
		beanConfig.setScan(true);
	}
}
