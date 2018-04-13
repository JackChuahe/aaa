package com.tank.aaa.entity;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolManager {
	private ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
	private ThreadPoolManager() {}
	
	public void execute(Runnable task){
		executor.execute(task);
		//Future future = executor.submit(task);
	}
	
	//Thread
	public int getPoolActiveThreadNum() {
		return executor.getActiveCount();
	}
	
	public int getAllThreadNum() {
		return Thread.activeCount();
	}
}
