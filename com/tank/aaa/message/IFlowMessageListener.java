package com.tank.aaa.message;

public interface IFlowMessageListener {
	/**
	 * flow message interface
	 * @param type
	 * @param msg
	 */
	public void messageRecive(FlowMessageType type, FlowMessage msg);
}
