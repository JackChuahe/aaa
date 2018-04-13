package com.tank.aaa.entity;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.pcap4j.packet.EthernetPacket;

public class BlockingQueueFactory {
	private static BlockingQueueFactory queue = new BlockingQueueFactory(); 
	private final static int BLOCK_QUEUE_MAX_SIZE = 20000; 

	private BlockingQueue<EthernetPacket> pktQueue = null;
	
	{   //initial
		pktQueue = new LinkedBlockingQueue<EthernetPacket>(BLOCK_QUEUE_MAX_SIZE);
	}

	public BlockingQueue<EthernetPacket> getPacketQueue(){
		return pktQueue;
	}
	
	//singleton Object 
	private BlockingQueueFactory() {}

	//queue factory
	public static BlockingQueueFactory getBlockingQueueFactory() {
		return queue;
	}
}
