package com.tank.aaa.entity;

import java.util.List;

import org.projectfloodlight.openflow.types.DatapathId;

public class FlowInfo {
	private int speed;
	private List<DatapathId> path;
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public List<DatapathId> getPath() {
		return path;
	}
	public void setPath(List<DatapathId> path) {
		this.path = path;
	}
	
	
	public String toString() {
		return ("speed: "+speed+" Path: "+path);
	}
}
