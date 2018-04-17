package com.tank.aaa.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class FlowMessageService implements IFloodlightModule, IFlowMessageService {
	private BlockingQueue<FlowMessage> bq = new LinkedBlockingQueue<FlowMessage>();
	private Map<FlowMessageType, List<IFlowMessageListenner>> listenners = new HashMap<FlowMessageType, List<IFlowMessageListenner>>();
	private static IThreadPoolService threadPoolService;
	private static final Logger log = LoggerFactory.getLogger(FlowMessageService.class);

	/**
	 * 
	 * @param msg
	 */
	@Override
	public void pubMessage(FlowMessage msg) {
		bq.offer(msg);
	}

	class NotifyReciver implements Runnable {

		@Override
		public void run() {
			log.info("Start up message dispatch service successful");
			while (true) {
				try {
					log.info("Waiting for message...");
					FlowMessage ifMsg = bq.take();
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
	@Override
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

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFlowMessageService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IFlowMessageService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IThreadPoolService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		threadPoolService.getScheduledExecutor().execute(new NotifyReciver());
		log.info("Starting up message dispatch service...");

	}
}
