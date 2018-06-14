package com.tank.aaa.entity;

import java.util.HashMap;
import java.util.Map;

public class Packet {
	private Flow flow;
	private Map<String, String> extraAttr = new HashMap<String, String>();

	public Flow getFlow() {
		return flow;
	}

	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	public String getExtraAttr(String key) {
		return extraAttr.get(key);
	}

	public void setExtraAttr(Map<String, String> extraAttr) {
		this.extraAttr = extraAttr;
	}

	public void addExtraAttr(String key, String val) {
		extraAttr.put(key, val);
	}

	@Override
	public String toString() {
		return flow.toString() + " - " + extraAttr.toString();
	}

}
