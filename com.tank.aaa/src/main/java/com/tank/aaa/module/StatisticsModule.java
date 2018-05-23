package com.tank.aaa.module;

import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IpNumber;

import com.tank.aaa.entity.BlockingQueueFactory;
import com.tank.aaa.entity.CaptureFlowManager;
import com.tank.aaa.entity.Flow;
import com.tank.aaa.util.AppContext;

public class StatisticsModule extends AbstractModule {
	private BlockingQueue<EthernetPacket> pktQueue = null;
	private Logger logger = LogManager.getLogger(StatisticsModule.class);
	private CaptureFlowManager cfm;

	private static int minClientPort = -1;
	private static int maxClientPort = -1;
	private static int minServerPort = -1;
	private static int maxServerPort = -1;

	@Override
	public void start() {

		loadStatsConfig();

		while (pktQueue != null) {
			try {
				EthernetPacket pkt = pktQueue.take();

				if (pkt.getHeader().getType().equals(EtherType.IPV4)) {
					processIpV4Packet((IpV4Packet) pkt.getPayload());
				}

			} catch (InterruptedException e) {
				logger.catching(e);
			}
		}
	}

	/**
	 * 
	 * @param packet
	 */
	private void processIpV4Packet(IpV4Packet packet) {
		if (packet.getHeader().getProtocol().equals(IpNumber.TCP)
				&& cfm.getCapturePropoerties().get("tcp").equals("true")) {
			cmixSmix(packet, true);

		} else if (packet.getHeader().getProtocol().equals(IpNumber.UDP)
				&& cfm.getCapturePropoerties().get("udp").equals("true")) {
			cmixSmix(packet, false);
		}
	}

	/**
	 * 
	 * @param packet
	 * @param isTcp
	 */
	private void cmixSmix(IpV4Packet packet, boolean isTcp) {
		if (isTcp) {
			TcpPacket tcp = (TcpPacket) packet.getPayload();

			if (((tcp.getHeader().getSrcPort().valueAsInt() >= minServerPort
					&& tcp.getHeader().getSrcPort().valueAsInt() <= maxServerPort)
					&& (tcp.getHeader().getDstPort().valueAsInt() >= minClientPort
							&& tcp.getHeader().getDstPort().valueAsInt() <= maxClientPort))
					|| ((tcp.getHeader().getSrcPort().valueAsInt() >= minClientPort
							&& tcp.getHeader().getSrcPort().valueAsInt() <= maxClientPort)
							&& (tcp.getHeader().getDstPort().valueAsInt() >= minServerPort
									&& tcp.getHeader().getDstPort().valueAsInt() <= maxServerPort))) {
				Flow flow = new Flow(packet.getHeader().getSrcAddr().hashCode(),
						packet.getHeader().getDstAddr().hashCode(), IpNumber.TCP.value(),
						tcp.getHeader().getSrcPort().valueAsInt(), tcp.getHeader().getDstPort().valueAsInt());
				cfm.recordFlow(flow);
			}
		} else {
			UdpPacket udp = (UdpPacket) packet.getPayload();

			if (((udp.getHeader().getSrcPort().valueAsInt() >= minServerPort
					&& udp.getHeader().getSrcPort().valueAsInt() <= maxServerPort)
					&& (udp.getHeader().getDstPort().valueAsInt() >= minClientPort
							&& udp.getHeader().getDstPort().valueAsInt() <= maxClientPort))
					|| ((udp.getHeader().getSrcPort().valueAsInt() >= minClientPort
							&& udp.getHeader().getSrcPort().valueAsInt() <= maxClientPort)
							&& (udp.getHeader().getDstPort().valueAsInt() >= minServerPort
									&& udp.getHeader().getDstPort().valueAsInt() <= maxServerPort))) {
				Flow flow = new Flow(packet.getHeader().getSrcAddr().hashCode(),
						packet.getHeader().getDstAddr().hashCode(), IpNumber.UDP.value(),
						udp.getHeader().getSrcPort().valueAsInt(), udp.getHeader().getDstPort().valueAsInt());
				cfm.recordFlow(flow);
			}
		}
	}

	@Override
	public void init(AppContext ctx) {
		pktQueue = ctx.getService(BlockingQueueFactory.class).getPacketQueue();
		if (pktQueue == null) {
			logger.error("get Packet Queue failed!");
		}
		cfm = new CaptureFlowManager(ctx);
	}

	/**
	 * 
	 */
	private void loadStatsConfig() {
		logger.info("Loading stats config...");

		Object o = cfm.getCapturePropoerties().get("min-client-port");
		if (o != null && !o.toString().equals("")) {
			minClientPort = Integer.parseInt(o.toString());
		}

		o = cfm.getCapturePropoerties().get("max-client-port");

		if (o != null && !o.toString().equals("")) {
			maxClientPort = Integer.parseInt(o.toString());
		}

		o = cfm.getCapturePropoerties().get("max-server-port");

		if (o != null && !o.toString().equals("")) {
			maxServerPort = Integer.parseInt(o.toString());
		}

		o = cfm.getCapturePropoerties().get("min-server-port");
		if (o != null && !o.toString().equals("")) {
			minServerPort = Integer.parseInt(o.toString());
		}

		logger.info("Loaded stats config finished!");
		logger.info("Filter condition: min-client-port: " + minClientPort + " max-client-port: " + maxClientPort + " minServerPort: "
				+ minServerPort + " maxServerport: " + maxServerPort);
		;
	}

	@Override
	public String getName() {
		return "Statistics Module";
	}

}
