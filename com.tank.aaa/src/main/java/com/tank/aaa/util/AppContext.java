package com.tank.aaa.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppContext {
	private Map<Class,Object> contextObjects = new HashMap<Class,Object>();
	private Logger logger = LogManager.getFormatterLogger(AppContext.class);

	private AppContext() {}

	public <T> T getService(Class<T> theClass) {
		if(contextObjects.containsKey(theClass)) {
			return (T)contextObjects.get(theClass);
		}
		try {
			Constructor con = theClass.getDeclaredConstructor();
			con.setAccessible(true);
			Object obj = con.newInstance();
			contextObjects.put(theClass, obj);
			return (T)obj;
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
			logger.catching(e);
			return null;
		}
	}
	

	public void exit(int status) {
		logger.info("System shutdown with code: " + status);
		System.exit(status);
	}
}
