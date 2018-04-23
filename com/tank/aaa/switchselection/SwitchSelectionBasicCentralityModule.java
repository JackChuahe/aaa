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
import com.tank.aaa.message.IFlowMessageListener;
import com.tank.aaa.message.IFlowMessageService;

import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.topology.NodePortTuple;

public class SwitchSelectionBasicCentralityModule
		implements IFloodlightModule, IFlowMessageListener, IOFSwitchListener, ISwitchSelectionService {
	protected static Logger logger;
	private IFlowMessageService flowMessageService;
	private IOFSwitchService switchService;

	public static final int FLOW_MAP_BASE_SIZE = 1000;
	public static final float FLOW_MAP_BASE_LOAD_FACTOR = (float) 1.0;

	public static final int SWITCH_MAP_BASE_SIZE = 200;
	public static final float SWITCH_MAP_BASE_LOAD_FACTOR = (float) 1.0;

	private Map<Flow, FlowInfo> flows = new HashMap<Flow, FlowInfo>(FLOW_MAP_BASE_SIZE, FLOW_MAP_BASE_LOAD_FACTOR);
	private Map<DatapathId, Set<Flow>> switchMatrix = new HashMap<DatapathId, Set<Flow>>(SWITCH_MAP_BASE_SIZE,
			SWITCH_MAP_BASE_LOAD_FACTOR);
	public Set<ISwitchSelectionUpdateListener> listeners = new HashSet<ISwitchSelectionUpdateListener>(10);

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(ISwitchSelectionService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(ISwitchSelectionService.class, this);
		return m;
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
			updateSwitchSelection();
		}

	}

	/**
	 * 
	 * @param msg
	 */
	public void dealWithFlowRemoveMsg(FlowRemovedMessage msg) {

		FlowStatics flowStatics = msg.getFlowStats();
		logger.info("removed: " + msg);
		for (DatapathId dpid : flows.get(flowStatics.getFlow()).getPath()) {
			switchMatrix.get(dpid).remove(flowStatics.getFlow());
		}
		flows.remove(flowStatics.getFlow());
		updateSwitchSelection();

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

	/**
	 * 
	 */
	public void updateSwitchSelection() {
		Map<Integer, DatapathId> switchMap = new HashMap<Integer, DatapathId>();
		Map<DatapathId, Integer> switchToInteger = new HashMap<DatapathId, Integer>();
		Map<Integer, Flow> flowMap = new HashMap<Integer, Flow>();
		Map<Flow, Integer> flowToInteger = new HashMap<Flow, Integer>();

		int cnt = 0;
		for (DatapathId dpid : switchMatrix.keySet()) {
			switchMap.put(cnt, dpid);
			switchToInteger.put(dpid, cnt);
			cnt++;
		}

		int[][] matrix = new int[flows.size() + 5][switchMatrix.size() + 5];

		cnt = 0;
		for (Flow flow : flows.keySet()) {
			flowMap.put(cnt, flow);
			flowToInteger.put(flow, cnt);
			for (DatapathId dpid : flows.get(flow).getPath()) {
				matrix[cnt][switchToInteger.get(dpid)]++;
			}
			cnt++;
		}

		//
		Set<Integer> set = new HashSet<Integer>();
		int max = 0;
		int maxDpid = -1;

		Set<DatapathId> switchSelected = new HashSet<DatapathId>();
		Map<DatapathId, Integer> switchCentrity = new HashMap<DatapathId, Integer>();

		do {
			max = 0;
			maxDpid = -1;
			for (int j = 0; j < matrix[0].length; ++j) {
				if (set.contains(j))
					continue;
				int sum = 0;
				for (int i = 0; i < matrix.length; ++i) {
					sum += matrix[i][j];
				}
				if (sum > max) {
					max = sum;
					maxDpid = j;
				}
			}
			if (maxDpid != -1) {
				set.add(maxDpid);
				switchSelected.add(switchMap.get(maxDpid));
				switchCentrity.put(switchMap.get(maxDpid), max);

				for (Flow flow : switchMatrix.get(switchMap.get(maxDpid))) {
					int idx = flowToInteger.get(flow);
					for (int i = 0; i < matrix[idx].length; ++i) {
						matrix[idx][i] = 0;
					}
				}
			}
		} while (maxDpid != -1);
		// logger.info(switchSelected.toString());
		notifySwitchSelectionUpdate(switchSelected, switchCentrity); // notify all listeners
	}

	/**
	 * 
	 * @param sws
	 */
	public void notifySwitchSelectionUpdate(Set<DatapathId> sws, Map<DatapathId, Integer> switchCentrity) {
		for (ISwitchSelectionUpdateListener listenner : listeners) {
			listenner.switchSelectionUpdate(sws,switchCentrity);
		}
	}

	/**
	 * 
	 * @param matrix
	 */
	public void printMatrixTest(int[][] matrix) {
		for (int i = 0; i < matrix.length; ++i) {
			for (int j = 0; j < matrix[i].length; ++j) {
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println();
		}
		System.out.println();
	}

	@Override
	synchronized public void addSwitchSelectionUpdateListener(ISwitchSelectionUpdateListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
}
