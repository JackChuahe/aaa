package com.tank.aaa.switchselection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tank.aaa.entity.Flow;
import com.tank.aaa.entity.FlowInfo;
import com.tank.aaa.entity.FlowStatics;
import com.tank.aaa.message.FlowAddMessage;
import com.tank.aaa.message.FlowMessage;
import com.tank.aaa.message.FlowMessageType;
import com.tank.aaa.message.FlowRemovedMessage;
import com.tank.aaa.message.IFlowMessageListenner;
import com.tank.aaa.message.IFlowMessageService;

import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.topology.NodePortTuple;

public class SwitchSelectionModule implements IFloodlightModule, IFlowMessageListenner, IOFSwitchListener {
	protected static Logger logger;
	private IFlowMessageService flowMessageService;
	private IOFSwitchService switchService;

	private static final int FLOW_MAP_BASE_SIZE = 1000;
	private static final float FLOW_MAP_BASE_LOAD_FACTOR = (float) 1.0;

	private static final int SWITCH_MAP_BASE_SIZE = 200;
	private static final float SWITCH_MAP_BASE_LOAD_FACTOR = (float) 1.0;

	private Map<Flow, FlowInfo> flows = new HashMap<Flow, FlowInfo>(FLOW_MAP_BASE_SIZE, FLOW_MAP_BASE_LOAD_FACTOR);
	private Map<DatapathId, Set<Flow>> switchMatrix = new HashMap<DatapathId, Set<Flow>>(SWITCH_MAP_BASE_SIZE,
			SWITCH_MAP_BASE_LOAD_FACTOR);

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFlowMessageService.class);
		l.add(IOFSwitchService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		flowMessageService = context.getServiceImpl(IFlowMessageService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		logger = LoggerFactory.getLogger(SwitchSelectionInfoCollector.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		flowMessageService.addFlowMessageListener(FlowMessageType.FLOW_ADD, this);
		flowMessageService.addFlowMessageListener(FlowMessageType.FLOW_REMOVE, this);
		switchService.addOFSwitchListener(this);
	}

	@Override
	public void messageRecive(FlowMessageType type, FlowMessage msg) {
		switch (type) {
		case FLOW_ADD:
			dealWithFlowAddMsg((FlowAddMessage) msg);
			break;
		case FLOW_REMOVE:
			dealWithFlowRemoveMsg((FlowRemovedMessage) msg);
			break;
		}
	}

	/**
	 * 
	 * @param msg
	 */
	public void dealWithFlowAddMsg(FlowAddMessage msg) {
		Flow flow = msg.getFlow();
		if (!flows.containsKey(flow)) {
			FlowInfo flowInfo = new FlowInfo();
			flows.put(flow, flowInfo);
			List<DatapathId> path = new ArrayList<DatapathId>();

			List<NodePortTuple> switchPortList = msg.getRoute().getPath();
			for (int indx = switchPortList.size() - 1; indx > 0; indx -= 2) {
				DatapathId switchDPID = switchPortList.get(indx).getNodeId();
				Set<Flow> fs = switchMatrix.get(switchDPID);
				fs.add(flow);
				path.add(switchDPID);
				// logger.info("fsSize: "+fs.size() + "");
			}
			flowInfo.setPath(path);
			// logger.info(path+"");
		}

	}

	/**
	 * 
	 * @param msg
	 */
	public void dealWithFlowRemoveMsg(FlowRemovedMessage msg) {

		FlowStatics flowStatics = msg.getFlowStats();
		for(DatapathId dpid : flows.get(flowStatics.getFlow()).getPath()) {
			switchMatrix.get(dpid).remove(flowStatics.getFlow());
			logger.info("removed: "+switchMatrix.entrySet());
		}
		flows.remove(flowStatics.getFlow());
		
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		switchUpdate(switchId);
	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		switchMatrix.remove(switchId);
	}

	@Override
	public void switchActivated(DatapathId switchId) {
		switchUpdate(switchId);
	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchChanged(DatapathId switchId) {
		switchUpdate(switchId);
	}

	public void switchUpdate(DatapathId switchId) {
		if (!switchMatrix.containsKey(switchId)) {
			switchMatrix.put(switchId, new HashSet<Flow>(FLOW_MAP_BASE_SIZE, FLOW_MAP_BASE_LOAD_FACTOR));
			logger.info(switchId.toString() + " switch add!");
		}
	}
}
