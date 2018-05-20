package com.tank.aaa.entity;

import java.util.List;

import org.projectfloodlight.openflow.types.DatapathId;

public class FlowInfo {
	private double duration; // seconds
	private double pkts; // how many packets per seconds (pkts/s)
	private double bps; // Byte per seconds (B/s)
	private long packetCount;
	private long byteCount;

	private List<DatapathId> path;

	public long getPacketCount() {
		return packetCount;
	}

	public void setPacketCount(long packetCount) {
		this.packetCount = packetCount;
	}

	public long getByteCount() {
		return byteCount;
	}

	public void setBytecount(long byteCount) {
		this.byteCount = byteCount;
	}

	public double getPkts() {
		return pkts;
	}

	public void setPkts(double pkts) {
		this.pkts = pkts;
	}

	/**
	 * 
	 * @return Byte per seconds
	 * 
	 */
	public double getBps() {
		return bps;
	}

	/**
	 * 
	 * @param bps
	 *            Byte per second
	 * 
	 */
	public void setBps(double bps) {
		this.bps = bps;
	}

	public List<DatapathId> getPath() {
		return path;
	}

	public void setPath(List<DatapathId> path) {
		this.path = path;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}

	public double getDuration() {
		return duration;
	}

	public String toString() {
		return ("pkts : " + pkts + " Bps: " + bps + " Path: " + path + " duration: " + duration);
	}
}
