package com.tank.aaa.sampling;

import org.projectfloodlight.openflow.types.DatapathId;

public class SwitchSamplingInfo {
	private DatapathId dpid;
	private long samplingTime;
	private long interval;

	public SwitchSamplingInfo() {
	}

	public DatapathId getDpid() {
		return dpid;
	}

	public void setDpid(DatapathId dpid) {
		this.dpid = dpid;
	}

	public long getSamplingTime() {
		return samplingTime;
	}

	public void setSamplingTime(long samplingTime) {
		this.samplingTime = samplingTime;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	@Override
	public String toString() {
		return "dpid: " + dpid.toString() + " samplingTime: " + samplingTime + " interval: " + interval;
	}
}
