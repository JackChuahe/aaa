package com.tank.aaa.module;

import com.tank.aaa.util.AppContext;

public interface BaseModule extends Runnable{
	/**
	 * start up this module
	 */
	public void start();
	/**
	 * when application start up ;u can initial some configuration
	 */
	public void init(AppContext ctx);
	
	
	/**
	 * set module name
	 * @return
	 */
	public String getName() ;
	
}
