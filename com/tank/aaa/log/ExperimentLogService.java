package com.tank.aaa.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class ExperimentLogService implements IExperimentLog, IFloodlightModule {
	private IThreadPoolService threadPoolService;
	private static final int TAG_SET_BASE_SIZE = 10;
	private static final float TAG_SET_LOAD_FACTOR = 1;
	private final static int BLOCK_QUEUE_MAX_SIZE = 20000;
	private static final Logger log = LoggerFactory.getLogger(ExperimentLogService.class);
	private Map<String, BufferedWriter> tags = new HashMap<String, BufferedWriter>(TAG_SET_BASE_SIZE,
			TAG_SET_LOAD_FACTOR);
	private LinkedBlockingQueue<Message> bq = new LinkedBlockingQueue<Message>(BLOCK_QUEUE_MAX_SIZE);
	private static final int TAKE_QUEUE_TIME_OUT = 100; // ms
	private static String baseDir = "";

	@Override
	public void addLoggerTag(String tag) {
		synchronized (this) {
			if (tag != null && !tag.equals(""))
				if (!tags.containsKey(tag)) {
					File file = new File(tag);
					try {
						BufferedWriter bf = new BufferedWriter(new FileWriter(file));
						tags.put(tag, bf);
						log.info("Add file tag: " + tag);
					} catch (IOException e) {
						log.error(e.getLocalizedMessage());
					}
				}

		}
	}

	@Override
	public void log(String content, String tag) {
		Message msg = new Message();
		msg.content = content;
		msg.tag = tag;
		bq.offer(msg);

		log.info("Pubish Msg: to" + tag);
	}

	class LogConsumer implements Runnable {

		@Override
		public void run() {
			log.info("LogConsumer Thread has start up!");
			while (true) {
				log.info("waiting for log message!");
				try {
					Message msg = bq.take();
					List<BufferedWriter> writerList = new ArrayList<BufferedWriter>(bq.size() + 15);

					do {
						BufferedWriter bf = tags.get(msg.tag);
						bf.write(msg.content);
						bf.newLine();
						writerList.add(bf);

						msg = bq.poll(TAKE_QUEUE_TIME_OUT, TimeUnit.MILLISECONDS);

					} while (msg != null);

					for (BufferedWriter toFlush : writerList) {
						toFlush.flush();
					}
				} catch (InterruptedException | IOException e) {
					log.error(e.getLocalizedMessage());
				}
			}
		}

	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IExperimentLog.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IExperimentLog.class, this);
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

		Map<String, String> configParameters = context.getConfigParams(this);
		String tmp = configParameters.get("/home/tank/log");
		if (tmp != null) {
			baseDir = tmp;
		}
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		threadPoolService.getScheduledExecutor().execute(new LogConsumer());
		log.info("ExperimentLogService has start up!");
	}

	static class Message {
		String content;
		String tag;
	}
}
