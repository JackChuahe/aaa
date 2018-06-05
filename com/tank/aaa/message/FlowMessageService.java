package com.tank.aaa.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import sun.util.logging.resources.logging;

public class FlowMessageService implements IFloodlightModule, IFlowMessageService {
	private final static int BLOCK_QUEUE_MAX_SIZE = 20000;
	private BlockingQueue<FlowMessage> flowAddRemovedBq = new LinkedBlockingQueue<FlowMessage>(BLOCK_QUEUE_MAX_SIZE);
	private BlockingQueue<FlowMessage> flowStatsBq = new LinkedBlockingQueue<FlowMessage>(BLOCK_QUEUE_MAX_SIZE);
	private Map<FlowMessageType, List<IFlowMessageListener>> listenners = new HashMap<FlowMessageType, List<IFlowMessageListener>>();
	private Map<IFlowMessageListener, Set<FlowMessageType>> listennerInfo = new HashMap<IFlowMessageListener, Set<FlowMessageType>>();
	private static IThreadPoolService threadPoolService;
	private static final Logger log = LoggerFactory.getLogger(FlowMessageService.class);
	private Thread t;
	private static final long WAITING_TIME = 100; // ms

	private final static int INIT_SIZE = 30;

	/**
	 * 
	 * @param msg
	 */
	@Override
	public void publishMessage(FlowMessage msg) {
		log.info(msg.getMessageType() + " enter queue");
		switch (msg.getMessageType()) {
		case FLOW_ADD:
			flowAddRemovedBq.offer(msg);
			break;
		case FLOW_REMOVE:
			flowAddRemovedBq.offer(msg);
			break;
		case FLOW_STATS_UPDATE:
			flowStatsBq.offer(msg);
			break;
		}
	}

	class NotifyFlowAddRemovedReciver implements Runnable {

		@Override
		public void run() {
			while (true) {
				try {
					log.info("aaaa");
					FlowMessage msg = flowAddRemovedBq.take();

					List<FlowMessage> addMsgs = new ArrayList<FlowMessage>(flowAddRemovedBq.size() + INIT_SIZE);
					List<FlowMessage> removedMsgs = new ArrayList<FlowMessage>(flowAddRemovedBq.size() + INIT_SIZE);
					List<FlowMessage> allMsgs = new ArrayList<FlowMessage>(flowAddRemovedBq.size() + INIT_SIZE);

					log.info("Flow Add & Removed Queue: Queue Size: " + flowAddRemovedBq.size());

					do {
						allMsgs.add(msg);
						if (msg.getMessageType() == FlowMessageType.FLOW_ADD) {
							addMsgs.add(msg);
						} else if (msg.getMessageType() == FlowMessageType.FLOW_REMOVE) {
							removedMsgs.add(msg);
						}
						msg = flowAddRemovedBq.poll(WAITING_TIME, TimeUnit.MILLISECONDS);
					} while (msg != null);

					//
					for (IFlowMessageListener listener : listennerInfo.keySet()) {
						Set<FlowMessageType> set = listennerInfo.get(listener);
						if (set.contains(FlowMessageType.FLOW_ADD) && set.contains(FlowMessageType.FLOW_REMOVE)) {
							log.info("OKOKOKOKOKOKOKOKOKOKOKOK");
							listener.messageRecive(allMsgs);
							log.info("OKOKOKOKOKOKOKOKOKOKOKOK");
						} else if (set.contains(FlowMessageType.FLOW_ADD)) {
							log.info("++++++++++++OKOKOKOKOKOKOKOKOKOKOKOK");
							listener.messageRecive(addMsgs);
							log.info("++++++++++++OKOKOKOKOKOKOKOKOKOKOKOK");
						} else if (set.contains(FlowMessageType.FLOW_REMOVE)) {
							log.info("============OKOKOKOKOKOKOKOKOKOKOKOK");
							listener.messageRecive(removedMsgs);
							log.info("============OKOKOKOKOKOKOKOKOKOKOKOK");
						}
					}

				} catch (InterruptedException e) {
					log.error(e.getMessage());
				}
			}

		}

	}

	class NotifyFlowStatsUpdateReciver implements Runnable {

		@Override
		public void run() {
			while (true) {
				try {
					log.info("bbbbb");
					FlowMessage msg = flowStatsBq.take();
					List<FlowMessage> msgs = new ArrayList<FlowMessage>(flowStatsBq.size() + INIT_SIZE);
					log.info("Flow Stats Queue: Queue Size: " + flowStatsBq.size());
					do {
						msgs.add(msg);
						msg = flowStatsBq.poll(WAITING_TIME, TimeUnit.MILLISECONDS);
					} while (msg != null);

					log.info("Flow Stats messages: Package Size: " + msgs.size());
					for (IFlowMessageListener listener : listenners.get(FlowMessageType.FLOW_STATS_UPDATE)) {
						listener.messageRecive(msgs);
					}
					log.info("Flow stats messages take finished.");
				} catch (InterruptedException e) {
					log.error(e.getMessage());
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
	synchronized public void addFlowMessageListener(FlowMessageType type, IFlowMessageListener listener) {
		List<IFlowMessageListener> list = listenners.get(type);
		if (list == null) {
			list = new ArrayList<IFlowMessageListener>();
			list.add(listener);
			listenners.put(type, list);
		} else {
			if (!list.contains(listener)) {
				list.add(listener);
			}
		}

		if (listennerInfo.containsKey(listener)) {
			Set<FlowMessageType> set = listennerInfo.get(listener);
			set.add(type);
		} else {
			Set<FlowMessageType> set = new HashSet<FlowMessageType>();
			set.add(type);
			listennerInfo.put(listener, set);
		}

		log.info(listener.getClass().getSimpleName() + " listen for " + type + " messages");
		log.info("listener Infos: " + listennerInfo);
		log.info("listeners: " + listenners);
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
		threadPoolService.getScheduledExecutor().execute(new NotifyFlowAddRemovedReciver());
		threadPoolService.getScheduledExecutor().execute(new NotifyFlowStatsUpdateReciver());
		log.info("Starting up message dispatch service...");

	}

}
