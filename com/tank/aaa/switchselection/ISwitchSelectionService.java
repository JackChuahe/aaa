package com.tank.aaa.switchselection;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface ISwitchSelectionService extends IFloodlightService {
	public void addSwitchSelectionUpdateListener(ISwitchSelectionUpdateListener listener);
}
