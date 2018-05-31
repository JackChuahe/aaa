package com.tank.aaa.entity;

import org.projectfloodlight.openflow.types.U64;

public class FlowStatics {
	private Flow flow;
	private long durationSec;
	private long durationNsec;
	private int idleTimeout;
	private int hardTimeout;
	private U64 packetCount;
	private U64 byteCount;

	public Flow getFlow() {
		return flow;
	}

	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	public long getDurationSec() {
		return durationSec;
	}

	public void setDurationSec(long durationSec) {
		this.durationSec = durationSec;
	}

	public long getDurationNsec() {
		return durationNsec;
	}

	public void setDurationNsec(long durationNsec) {
		this.durationNsec = durationNsec;
	}

	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public FlowStatics(Flow flow, long durationSec, long durationNsec, int idleTimeout, int hardTimeout,
			U64 packetCount, U64 byteCount) {
		super();
		this.flow = flow;
		this.durationSec = durationSec;
		this.durationNsec = durationNsec;
		this.idleTimeout = idleTimeout;
		this.hardTimeout = hardTimeout;
		this.packetCount = packetCount;
		this.byteCount = byteCount;
	}

	public int getHardTimeout() {
		return hardTimeout;
	}

	public void setHardTimeout(int hardTimeout) {
		this.hardTimeout = hardTimeout;
	}

	public U64 getPacketCount() {
		return packetCount;
	}

	public void setPacketCount(U64 packetCount) {
		this.packetCount = packetCount;
	}

	public U64 getByteCount() {
		return byteCount;
	}

	public void setByteCount(U64 byteCount) {
		this.byteCount = byteCount;
	}

	public String toString() {
		return flow + "; durationSec: " + durationSec + ";durationNsec: " + durationNsec + ";idleTimeout: "
				+ idleTimeout + "; hardTimeout: " + hardTimeout + "; packetCount: " + packetCount + "; byteCount: "
				+ byteCount;
	}
}
