package com.tank.aaa.switchselection;

import java.util.List;

import org.projectfloodlight.openflow.types.DatapathId;

public interface ISwitchSelectionUpdateListener {
	public void switchSelectionUpdate(List<DatapathId> sws);
}
