package com.tank.aaa.module;

import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pcap4j.packet.EthernetPacket;

import com.tank.aaa.entity.BlockingQueueFactory;
import com.tank.aaa.util.AppContext;

public class StatisticsModule extends AbstractModule{
	private BlockingQueue<EthernetPacket> pktQueue = null;
	private Logger logger = LogManager.getLogger(StatisticsModule.class);

	@Override
	public void start() {
		while(pktQueue != null) {
			try {
				EthernetPacket pkt = pktQueue.take();
				System.out.println(pkt.getHeader().getSrcAddr());
			} catch (InterruptedException e) {
				logger.catching(e);
			}
		}
	}

	@Override
	public void init(AppContext ctx) {
		pktQueue = ctx.getService(BlockingQueueFactory.class).getPacketQueue();
		if(pktQueue == null) {
			logger.error("get Packet Queue failed!");
		}
	}

	@Override
	public String getName() {
		return "Statistics Module";
	}

}
