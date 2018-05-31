package com.tank.aaa.switchselection;

import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;

public interface ISwitchSelectionUpdateListener {
	public void switchSelectionUpdate(Set<DatapathId> sws,Map<DatapathId,Integer> switchCentrity);
}
