package com.tank.aaa.switchselection;

import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;

import com.tank.aaa.entity.Flow;
import com.tank.aaa.entity.FlowInfo;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface ISwitchSelectionService extends IFloodlightService {
	public void addSwitchSelectionUpdateListener(ISwitchSelectionUpdateListener listener);

	public Map<Flow, FlowInfo> getFlowInformation();

	public Map<DatapathId, Set<Flow>> getFlowMatrix();
	
	public Object getMutex();
}
