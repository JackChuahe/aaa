package com.tank.aaa.module;

import java.io.EOFException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.Packet;

import com.tank.aaa.entity.BlockingQueueFactory;
import com.tank.aaa.util.AppContext;
import com.tank.aaa.util.PropertiesLoader;

public class CaptureModule extends AbstractModule {
	private Logger logger = LogManager.getLogger(CaptureModule.class);
	private AppContext ctx = null;
	private PcapHandle handle = null;
	private BlockingQueue<EthernetPacket> pktQueue = null;

	@Override
	public void start() {
		while (true) {
			Packet packet;
			try {
				packet = handle.getNextPacketEx();
				EthernetPacket eth = packet.get(EthernetPacket.class);
				enQueue(eth);
			} catch (EOFException | PcapNativeException | TimeoutException | NotOpenException e) {
				logger.catching(e);
			}
		}
	}

	/**
	 * en queeue
	 * 
	 * @param eth
	 */
	public void enQueue(EthernetPacket eth) {
		pktQueue.offer(eth); // no wait to insert to queue
		// System.out.println(pktQueue.size());
	}

	@Override
	public void init(AppContext ctx) {
		logger.info("Initialing Capture module...");
		// loading configuration
		this.ctx = ctx;
		PropertiesLoader pl = ctx.getService(PropertiesLoader.class);
		Map properties = pl.loadProperties("pcapconfig.propertites");

		try {
			InetAddress addr = InetAddress.getByName(properties.get("bingDevicename").toString());
			PcapNetworkInterface nif = Pcaps.getDevByAddress(addr);// Pcaps.getDevByName("enp4s0f1");
			int snapLen = Integer.parseInt(properties.get("captureLen").toString());
			PromiscuousMode mode = PromiscuousMode.PROMISCUOUS;
			int timeout = Integer.parseInt(properties.get("timeout").toString());
			handle = nif.openLive(snapLen, mode, timeout);
			logger.info(properties.get("filter"));
			if (properties.get("filter") != null) {
				handle.setFilter(properties.get("filter").toString(), BpfCompileMode.OPTIMIZE);
			}
			logger.info("set handle finished. On: " + addr + " packet size: " + snapLen + " timeout: " + timeout
					+ " filter: " + properties.get("filter"));
		} catch (UnknownHostException | PcapNativeException | NotOpenException e1) {
			logger.catching(e1);
		}
		// load queue
		pktQueue = ctx.getService(BlockingQueueFactory.class).getPacketQueue();
		if (pktQueue == null) {
			logger.error("Get Blocking queue failed!");
			ctx.exit(-1);
		}
		logger.info("Initial Capture module finished!");
	}

	/**
	 * close
	 */
	public void handleClose() {
		if (handle != null && handle.isOpen()) {
			handle.close();
			logger.info("Capture handle closed!");
		}
	}

	@Override
	public String getName() {
		return "CaptureModule";
	}

}
