package com.tank.aaa.message;

public interface IFlowMessageListenner {
	/**
	 * flow message interface
	 * @param type
	 * @param msg
	 */
	public void messageRecive(FlowMessageType type, IFlowMessage msg);
}
