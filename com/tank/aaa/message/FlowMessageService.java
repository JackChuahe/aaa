package com.tank.aaa.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FlowMessageService {
	private BlockingQueue<IFlowMessage> bq = new LinkedBlockingQueue<IFlowMessage>();
	private Map<FlowMessageType, List<IFlowMessageListenner>> listenners = new HashMap<FlowMessageType, List<IFlowMessageListenner>>();

	/**
	 * 
	 * @param msg
	 */
	public void pubMessage(IFlowMessage msg) {
		bq.offer(msg);
	}

	class NotifyReciver implements Runnable {

		@Override
		public void run() {
			while (true) {
				try {
					IFlowMessage ifMsg = bq.take();
					for (IFlowMessageListenner listenner : listenners.get(ifMsg.getMessageType())) {
						listenner.messageRecive(ifMsg.getMessageType(), ifMsg);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * 
	 * @param type
	 * @param listenner
	 */
	synchronized public void addFlowMessageListener(FlowMessageType type, IFlowMessageListenner listenner) {
		List<IFlowMessageListenner> list = listenners.get(type);
		if (list == null) {
			list = new ArrayList<IFlowMessageListenner>();
			listenners.put(type, list);
		} else {
			if (!list.contains(listenner)) {
				list.add(listenner);
			}
		}
	}
}
