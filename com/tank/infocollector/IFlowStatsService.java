package com.tank.infocollector;

import java.util.Map;

import com.tank.aaa.entity.Flow;
import com.tank.aaa.entity.FlowInfo;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IFlowStatsService extends IFloodlightService {
	public Map<Flow,FlowInfo> getFlowStats();
	public void addFlowStatsUpdateListener(FlowStatsUpdateListener listener);
}
