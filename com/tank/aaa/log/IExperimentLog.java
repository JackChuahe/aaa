package com.tank.aaa.log;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IExperimentLog extends IFloodlightService {
	/**
	 * 
	 * @param tag
	 */
	public void addLoggerTag(String tag);

	/**
	 * 
	 * @param content
	 * @param tag
	 */
	public void log(String content, String tag);
}
