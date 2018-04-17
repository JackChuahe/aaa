package com.tank.aaa.message;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IFlowMessageService extends IFloodlightService{
	public void pubMessage(FlowMessage msg);
	public void addFlowMessageListener(FlowMessageType type, IFlowMessageListenner listenner) ;
}
