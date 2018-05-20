package com.tank.aaa.sampling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tank.aaa.switchselection.ISwitchSelectionService;
import com.tank.aaa.switchselection.ISwitchSelectionUpdateListener;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class HashBasedUniformSampling implements IFloodlightModule, ISwitchSelectionUpdateListener, IOFSwitchListener {
	private ISwitchSelectionService switchSelectionService;
	private IOFSwitchService switchService;
	private static final Logger logger = LoggerFactory.getLogger(HashBasedUniformSampling.class);
	private BlockingQueue<List<DatapathId>> bq = new LinkedBlockingQueue<List<DatapathId>>();
	private int dpiMaxRate = 0;
	private Set<DatapathId> currentSamplingSws = new HashSet<DatapathId>();
	private final OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);

	@Override
	public void switchSelectionUpdate(Set<DatapathId> sws, Map<DatapathId, Integer> switchCentrity) {
		logger.info("Switch sampling update: " + sws);
		int total = 0;
		for (DatapathId dpid : switchCentrity.keySet()) {
			total += switchCentrity.get(dpid);
		}

		for (DatapathId dpid : sws) {
			if (total != 0) {
				int samplingRate = (int) Math.floor((double) (dpiMaxRate * switchCentrity.get(dpid)) / total);
				sendUpdateSamplingRate(dpid, 10, 1/* - samplingRate */);
				System.out.println(
						"Sampling rate: " + dpid + ": Sampling VS Drop:[" + samplingRate + " - " + (dpiMaxRate) + "]");
			}
		}

		currentSamplingSws.removeAll(sws);

		for (DatapathId dpid : currentSamplingSws) {
			sendUpdateSamplingRate(dpid, 0, 1); // drop all
		}
		currentSamplingSws = sws;

	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(ISwitchSelectionService.class);
		l.add(IFloodlightProviderService.class);
		l.add(IOFSwitchService.class);

		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		switchSelectionService = context.getServiceImpl(ISwitchSelectionService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);

		Map<String, String> configParameters = context.getConfigParams(this);
		String tmp = configParameters.get("dpi-rate");
		if (tmp != null && tmp != "") {
			dpiMaxRate = Integer.parseInt(tmp);
		}
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		switchSelectionService.addSwitchSelectionUpdateListener(this);
		switchService.addOFSwitchListener(this);
		logger.info("Hash Based Sampling module start up !");
	}

	/**
	 * 
	 * @param dpid
	 * @param samplingWeight
	 * @param dropWeight
	 */
	public void sendUpdateSamplingRate(DatapathId dpid, int samplingWeight, int dropWeight) {
		List<OFBucket> buckets = generateBuckets(samplingWeight, dropWeight);
		switchService.getSwitch(dpid).write(factory.buildGroupModify().setGroup(OFGroup.of(0x01))
				.setGroupType(OFGroupType.INDIRECT).setBuckets(buckets).build());
	}

	@Override
	public void switchAdded(DatapathId switchId) {

		List<OFBucket> buckets = generateBuckets(0, 100);
		switchService.getSwitch(switchId).write(factory.buildGroupAdd().setGroupType(OFGroupType.INDIRECT)
				.setGroup(OFGroup.of(0x01)).setBuckets(buckets).build());

		logger.info("HashBased send group add to switch: " + switchId);

	}

	/**
	 * 
	 * @param samplingWeight
	 * @param dropWeight
	 * @return
	 */
	private List<OFBucket> generateBuckets(int samplingWeight, int dropWeight) {
		List<OFBucket> buckets = new ArrayList<OFBucket>();
		List<OFAction> actions = new ArrayList<OFAction>();
		OFActionOutput.Builder outPutDPISystemPort = factory.actions().buildOutput();
		outPutDPISystemPort.setPort(OFPort.of(3));
		outPutDPISystemPort.setMaxLen(Integer.MAX_VALUE);
		actions.add(outPutDPISystemPort.build());

		List<OFAction> bucketDropAction = new ArrayList<OFAction>();

		if (samplingWeight != 0) {

			OFBucket bkForwardToDPI = factory.buildBucket().setActions(actions)/*.setWeight(samplingWeight)*/
					.setWatchGroup(OFGroup.ANY).setWatchPort(OFPort.ANY).build();
			buckets.add(bkForwardToDPI);
		} else {

			OFBucket bkDrop = factory.buildBucket().setActions(bucketDropAction)/*.setWeight(dropWeight)*/
					.setWatchGroup(OFGroup.ANY).setWatchPort(OFPort.ANY).build();

			buckets.add(bkDrop);

		}
		return buckets;
	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchChanged(DatapathId switchId) {
		// TODO Auto-generated method stub

	}
}
