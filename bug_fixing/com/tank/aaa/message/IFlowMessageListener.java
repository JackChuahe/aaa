package com.tank.aaa.message;

import java.util.List;

public interface IFlowMessageListener {
	/**
	 * flow message interface
	 * 
	 * @param type
	 * @param msg
	 */
	public void messageRecive(List<FlowMessage> msgs);
}
