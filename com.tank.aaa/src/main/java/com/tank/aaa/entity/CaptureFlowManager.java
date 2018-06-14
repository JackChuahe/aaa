package com.tank.aaa.entity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tank.aaa.module.StatisticsModule;
import com.tank.aaa.util.AppContext;
import com.tank.aaa.util.PropertiesLoader;

public class CaptureFlowManager {
	private Logger logger = LogManager.getLogger(StatisticsModule.class);
	public static final int FLOW_MAP_BASE_SIZE = 1000;
	public static final float FLOW_MAP_LOAD_FACTOR = 1;

	private static final int SET_BASE_SIZE = 220;
	private static final float SET_LOAD_FACTOR = 1;

	private Map<Flow, Long> flowStats = Collections
			.synchronizedMap(new HashMap<Flow, Long>(FLOW_MAP_BASE_SIZE, FLOW_MAP_LOAD_FACTOR));
	private AppContext ctx;
	private Map captureProperties;
	private int totalPkt = 0;
	private int repeatPkts = 0;
	private Map<Flow, Set<String>> uniquePktSet = new HashMap<Flow, Set<String>>(FLOW_MAP_BASE_SIZE,
			FLOW_MAP_LOAD_FACTOR);

	public CaptureFlowManager(AppContext ctx) {
		this.ctx = ctx;
		ThreadPoolManager threadPool = ctx.getService(ThreadPoolManager.class);
		if (threadPool != null) {
			threadPool.execute(new asynToFile());
			threadPool.execute(new asynSpeedCalculate());
			threadPool.execute(new asynRepeatRatePkt());
		}
	}

	public Map getCapturePropoerties() {
		if (captureProperties == null) {
			captureProperties = ctx.getService(PropertiesLoader.class).loadProperties("flow_stats.config");
		}

		return captureProperties;
	}

	class asynRepeatRatePkt implements Runnable {

		@Override
		public void run() {
			logger.info("Asyn calculate Repeat rate to has start up!");
			OutputStream out;
			File f = new File("collectorlog/packetRepeatRate.log");
			try {

				if (!f.exists()) {
					f.createNewFile();
				} else {
					f.delete();
					f.createNewFile();
				}

				logger.info("Repeat rate log file to: " + f.getAbsolutePath());
				out = new FileOutputStream(f);
				BufferedWriter bfw = new BufferedWriter(new OutputStreamWriter(out));

				while (true) {
					try {
						Thread.sleep(10 * 1000);
					} catch (InterruptedException e) {
						logger.catching(e);
					}

					if (totalPkt == 0.0) {
						continue;
					}
					bfw.write(totalPkt + " " + repeatPkts + " "
							+ String.format("%.4f", ((double) (repeatPkts) / (double) (totalPkt))));
					bfw.newLine();
					bfw.flush();
				}

			} catch (Exception e) {
				logger.catching(e);
			}
		}

	}

	class asynSpeedCalculate implements Runnable {

		@Override
		public void run() {
			logger.info("Asyn calculate Speed to has start up!");
			int oldPkt = 0;
			OutputStream out;
			File f = new File("collectorlog/speed.log");
			try {

				if (!f.exists()) {
					f.createNewFile();
				} else {
					f.delete();
					f.createNewFile();
				}

				logger.info("Speed log file to: " + f.getAbsolutePath());
				out = new FileOutputStream(f);
				BufferedWriter bfw = new BufferedWriter(new OutputStreamWriter(out));

				while (true) {
					try {
						Thread.sleep(3100);
					} catch (InterruptedException e) {
						logger.catching(e);
					}

					bfw.write(String.format("%.4f pkt/s", ((double) (totalPkt - oldPkt) / 3.1)));
					bfw.newLine();
					bfw.flush();
					oldPkt = totalPkt;
				}

			} catch (Exception e) {
				logger.catching(e);
			}

		}
	}

	class asynToFile implements Runnable {

		@Override
		public void run() {
			logger.info("Asyn to file has start up!");

			while (true) {
				try {
					Thread.sleep(30 * 1000); // sleep 40 seconds
				} catch (InterruptedException e) {
					logger.catching(e);
				}

				OutputStream out;
				try {
					File f = new File("collectorlog/flowRecoder.log");
					if (!f.exists()) {
						f.createNewFile();
					} else {
						f.delete();
						f.createNewFile();
					}

					logger.info("flowRecoder.log file to: " + f.getAbsolutePath());
					out = new FileOutputStream(f);
					BufferedWriter bfw = new BufferedWriter(new OutputStreamWriter(out));
					logger.info("Start to log flow stats to file ...");
					synchronized (flowStats) {
						for (Flow flow : flowStats.keySet()) {
							bfw.write(flow.toString());
							bfw.write(" " + flowStats.get(flow));
							bfw.newLine();
						}
					}
					bfw.flush();
					bfw.close();
					logger.info("Log flow stats to file finished!");

				} catch (IOException e) {
					logger.catching(e);
				}

			}
		}

	}

	/**
	 * 
	 * @param flow
	 * @return true: if add a new flow. false: if the flow has exists and packet
	 *         number +1
	 */
	public boolean recordFlow(Packet packet) {
		recordFlow(packet.getFlow());
		totalPkt++;
		String headerHashCode = packet.getExtraAttr("headerHashCode");
		Set<String> set = uniquePktSet.get(packet.getFlow());
		if (set == null) {
			set = new HashSet<String>(SET_BASE_SIZE, SET_LOAD_FACTOR);
			uniquePktSet.put(packet.getFlow(), set);
		}

		if (set.contains(headerHashCode)) {
			repeatPkts++;
		} else {
			set.add(headerHashCode);
		}

		return true;
	}

	public boolean recordFlow(Flow flow) {
		if (flowStats.containsKey(flow)) {
			flowStats.put(flow, flowStats.get(flow) + 1);
			return false;
		} else {
			flowStats.put(flow, (long) 1);
		}
		return true;
	}

	public boolean isContain(Flow flow) {
		return flowStats.containsKey(flow);
	}
}
