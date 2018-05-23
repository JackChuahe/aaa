package com.tank.aaa.entity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tank.aaa.module.StatisticsModule;
import com.tank.aaa.util.AppContext;
import com.tank.aaa.util.PropertiesLoader;

public class CaptureFlowManager {
	private Logger logger = LogManager.getLogger(StatisticsModule.class);
	public static final int FLOW_MAP_BASE_SIZE = 1000;
	public static final float FLOW_MAP_LOAD_FACTOR = 1;

	private Map<Flow, Long> flowStats = new HashMap<Flow, Long>(FLOW_MAP_BASE_SIZE, FLOW_MAP_LOAD_FACTOR);
	private AppContext ctx;
	private Map captureProperties;

	public CaptureFlowManager(AppContext ctx) {
		this.ctx = ctx;
		ThreadPoolManager threadPool = ctx.getService(ThreadPoolManager.class);
		if (threadPool != null) {
			threadPool.execute(new asynToFile());
		}
	}

	public Map getCapturePropoerties() {
		if (captureProperties == null) {
			captureProperties = ctx.getService(PropertiesLoader.class).loadProperties("flow_stats.config");
		}

		return captureProperties;
	}

	class asynToFile implements Runnable {

		@Override
		public void run() {
			logger.info("Asyn to file has start up!");

			while (true) {
				try {
					Thread.sleep(40 * 1000); // sleep 40 seconds
				} catch (InterruptedException e) {
					logger.catching(e);
				}

				OutputStream out;
				try {
					File f = new File("flowRecoder.log");
					if (!f.exists()) {
						f.createNewFile();
					} else {
						f.delete();
						f.createNewFile();
					}

					logger.info("flowRecoder.log file to: " + f.getAbsolutePath());
					out = new FileOutputStream("flowRecoder.log");
					BufferedWriter bfw = new BufferedWriter(new OutputStreamWriter(out));
					logger.info("Start to log flow stats to file ...");
					for (Flow flow : flowStats.keySet()) {
						bfw.write(flow.toString());
						bfw.write(" " + flowStats.get(flow));
						bfw.newLine();
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
