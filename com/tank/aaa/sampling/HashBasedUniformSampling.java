package com.tank.aaa.sampling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFGroupModify;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tank.aaa.entity.Flow;
import com.tank.aaa.entity.FlowInfo;
import com.tank.aaa.switchselection.ISwitchSelectionService;
import com.tank.aaa.switchselection.ISwitchSelectionUpdateListener;
import com.tank.infocollector.FlowStatsUpdateListener;
import com.tank.infocollector.IFlowStatsService;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class HashBasedUniformSampling implements IFloodlightModule, ISwitchSelectionUpdateListener, IOFSwitchListener,
		TriggerListener, FlowStatsUpdateListener {
	private ISwitchSelectionService switchSelectionService;
	private IFlowStatsService flowStatsService;
	public static final Logger logger = LoggerFactory.getLogger(HashBasedUniformSampling.class);
	private int dpiMaxRate = 0;
	private static final int SWITCH_MAP_BASE_SIZE = 200;
	private static final float SWITCH_MAP_BASE_LOAD_FACTOR = 1;

	private static Set<String> currentSamplingSws = Collections
			.synchronizedSet(new HashSet<String>(SWITCH_MAP_BASE_SIZE, SWITCH_MAP_BASE_LOAD_FACTOR));

	private static Map<String, SwitchSamplingInfo> mapForSwitchSamplingInfo = Collections
			.synchronizedMap(Collections.synchronizedMap(
					new HashMap<String, SwitchSamplingInfo>(SWITCH_MAP_BASE_SIZE, SWITCH_MAP_BASE_LOAD_FACTOR)));

	private final OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);

	private Scheduler scheduler;

	public static final List<OFAction> BUCKET_TO_DROP_ACTION = new ArrayList<OFAction>();
	public static final List<OFAction> BUCKET_TO_FORWARD_ACTION = new ArrayList<OFAction>();
	private static IOFSwitchService switchService;

	public static OFGroupModify SamplingMsg = null;
	public static OFGroupModify DropMsg = null;

	public static Set<String> workingSwitches = Collections
			.synchronizedSet(new HashSet<String>(SWITCH_MAP_BASE_SIZE, SWITCH_MAP_BASE_LOAD_FACTOR));

	@Override
	public void switchSelectionUpdate(Set<DatapathId> sws, Map<DatapathId, Integer> switchCentrity) {

		logger.info("Switch sampling update: " + sws);

		clearJobs(); // clear jobs
		calculateSamplingRateForSwitches(sws); // compute new sampling strategy
		stopSamplingSwitchs(); // stop sampling switches
		startNewSampling(sws);
	}

	/**
	 * clear doing jobs
	 */
	public void clearJobs() {
		logger.info("Clear jobs");
		try {
			if (!scheduler.isStarted())
				return;

			workingSwitches.clear();// clear all are working switches in quartz
			scheduler.shutdown(true);
			scheduler = StdSchedulerFactory.getDefaultScheduler();

		} catch (SchedulerException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * start for new round sampling
	 */
	public void startNewSampling(Set<DatapathId> sws) {
		// start job
		logger.info("start new sampling scheduler...");
		for (DatapathId dpid : sws) {

			JobDetail samplingJob = JobBuilder.newJob(SamplingJob.class).usingJobData("dpid", dpid.toString())
					.withIdentity("job-" + "-sampling-" + dpid.toString(), "group").build();

			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity("trigger-" + "-sampling-" + dpid.toString(), "group").startNow()// startAt(new
																									// Date(ctime + 10))
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0)) // for
																														// only
																														// once
					.build();
			try {
				scheduler.scheduleJob(samplingJob, trigger);
			} catch (SchedulerException e) {
				logger.error(e.getMessage());
			}
		}

		try {

			if (!scheduler.isStarted() && sws.size() > 0) {
				scheduler.start();
				logger.info("start sampling scheduler...");
			}
		} catch (SchedulerException e) {
			logger.error(e.getMessage());
		}

	}

	/**
	 * stop sampling switchs
	 * 
	 */
	public void stopSamplingSwitchs() {
		logger.info("Stop sampling switches");
		for (String dpid : currentSamplingSws) {
			switchService.getSwitch(mapForSwitchSamplingInfo.get(dpid).getDpid()).write(DropMsg);
			logger.info("send stop sampling msg to: " + dpid);
			// stop sampling
		}
		currentSamplingSws.clear();
	}

	/**
	 * compute sampling rate for switches
	 */
	public void calculateSamplingRateForSwitches(Set<DatapathId> sws) {
		logger.info("calculation sampling rate");
		mapForSwitchSamplingInfo.clear();
		Map<DatapathId, Set<Flow>> switchMatrix = switchSelectionService.getFlowMatrix();
		Map<String, Double> tmp = new HashMap<String, Double>(mapForSwitchSamplingInfo.size(),
				SWITCH_MAP_BASE_LOAD_FACTOR);
		Map<Flow, FlowInfo> mapForFlowInfo = flowStatsService.getFlowStats();

		double total = 0.0;
		for (DatapathId dpid : sws) {
			double swPktps = 0.0;
			for (Flow flow : switchMatrix.get(dpid)) {

				//logger.info(mapForFlowInfo.toString());
				FlowInfo flowInfo = mapForFlowInfo.get(flow);
				if (flowInfo != null) {
					total += flowInfo.getPkts();
					swPktps += flowInfo.getPkts();
				}
			}
			tmp.put(dpid.toString(), swPktps);
		}

		// calculate sampling rate
		for (DatapathId dpid : sws) {
			SwitchSamplingInfo swInfo = new SwitchSamplingInfo();
			double swRate = tmp.get(dpid.toString());
			swInfo.setDpid(dpid);

			if (total < 10e-5 || swRate < 10e-5) {
				swInfo.setInterval(0);
				swInfo.setSamplingTime(0);
			} else {
				double rate = swRate / total;
				double avliableRate = rate * dpiMaxRate;
				if (avliableRate >= swRate) {
					swInfo.setInterval(0);
				} else {
					double samplingRate = avliableRate / swRate;
					long samplingTime = (long) (samplingRate * 1000);

					// logger.info("sampling rate: " + samplingRate + " sampling time: " +
					// samplingTime);
					swInfo.setInterval(((long) 1000 - samplingTime));
					swInfo.setSamplingTime(samplingTime);
				}
			}

			mapForSwitchSamplingInfo.put(dpid.toString(), swInfo);

			// logger.info("Calculation: " + dpid + "- samplingTime: "
			// + mapForSwitchSamplingInfo.get(dpid.toString()).getSamplingTime() + "
			// Interval: "
			// + mapForSwitchSamplingInfo.get(dpid.toString()).getInterval());
		}
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
		l.add(IFlowStatsService.class);

		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		switchSelectionService = context.getServiceImpl(ISwitchSelectionService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		flowStatsService = context.getServiceImpl(IFlowStatsService.class);

		try {// initial scheduler
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.getListenerManager().addTriggerListener(this);
		} catch (SchedulerException e) {
			logger.error(e.getMessage());
		}

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
		flowStatsService.addFlowStatsUpdateListener(this);

		// set forward action
		OFActionOutput.Builder outPutDPISystemPort = factory.actions().buildOutput();
		outPutDPISystemPort.setPort(OFPort.of(0x01));
		outPutDPISystemPort.setMaxLen(Integer.MAX_VALUE);
		BUCKET_TO_FORWARD_ACTION.add(outPutDPISystemPort.build());

		List<OFBucket> buckets = generateBuckets(false);
		SamplingMsg = factory.buildGroupModify().setGroup(OFGroup.of(0x01)).setGroupType(OFGroupType.INDIRECT)
				.setBuckets(buckets).build();

		List<OFBucket> dropBuckets = generateBuckets(true);
		DropMsg = factory.buildGroupModify().setGroup(OFGroup.of(0x01)).setGroupType(OFGroupType.INDIRECT)
				.setBuckets(dropBuckets).build();

		logger.info("Hash Based Sampling module start up !");
	}

	/**
	 * 
	 * @param dpid
	 * @param samplingWeight
	 * @param dropWeight
	 */
	public void sendUpdateSamplingRate(DatapathId dpid, boolean isDrop) {
		if (isDrop) {
			switchService.getSwitch(dpid).write(DropMsg);
		} else {
			switchService.getSwitch(dpid).write(SamplingMsg);
		}
	}

	@Override
	public void switchAdded(DatapathId switchId) {

		List<OFBucket> buckets = generateBuckets(true);
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
	private List<OFBucket> generateBuckets(boolean isDrop) {
		List<OFBucket> buckets = new ArrayList<OFBucket>();

		if (!isDrop) {// sampling

			OFBucket bkForwardToDPI = factory.buildBucket().setActions(BUCKET_TO_FORWARD_ACTION)
					.setWatchGroup(OFGroup.ANY).setWatchPort(OFPort.ANY).build();
			buckets.add(bkForwardToDPI);

		} else {// stop sampling -> drop packet

			OFBucket bkDrop = factory.buildBucket().setActions(BUCKET_TO_DROP_ACTION)/* .setDrop action () */
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

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return this.getClass().getSimpleName();
	}

	@Override
	public void triggerComplete(Trigger arg0, JobExecutionContext arg1, CompletedExecutionInstruction arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void triggerFired(Trigger arg0, JobExecutionContext arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void triggerMisfired(Trigger arg0) {
		System.err.println("Miss fired!");

	}

	@Override
	public boolean vetoJobExecution(Trigger arg0, JobExecutionContext arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * get switch sampling information
	 * 
	 * @return Map<String,SwitchSamplingInfo>
	 */
	public static Map<String, SwitchSamplingInfo> getSwitchSamplingInfo() {
		return mapForSwitchSamplingInfo;
	}

	/**
	 * get current samplng switches
	 * 
	 * @return Set<String> String: dpid.toString()
	 */
	public static Set<String> getCurrentSamplingSwitches() {
		return currentSamplingSws;
	}

	/**
	 * 
	 * @return IOFSwitchService
	 */
	public static IOFSwitchService getSwitchService() {
		return switchService;
	}

	// listen for notify
	@Override
	public void flowStatsUpdate() {
		Map<DatapathId, Set<Flow>> switchMatrix = switchSelectionService.getFlowMatrix();
		Map<String, Double> tmp = new HashMap<String, Double>(mapForSwitchSamplingInfo.size(),
				SWITCH_MAP_BASE_LOAD_FACTOR);
		Map<Flow, FlowInfo> mapForFlowInfo = flowStatsService.getFlowStats();

		logger.info(mapForFlowInfo.toString());

		double total = 0.0;
		for (String dpid : mapForSwitchSamplingInfo.keySet()) {
			double swPktps = 0.0;
			for (Flow flow : switchMatrix.get(mapForSwitchSamplingInfo.get(dpid).getDpid())) {
				FlowInfo flowInfo = mapForFlowInfo.get(flow);
				if (flowInfo != null) {
					total += flowInfo.getPkts();
					swPktps += flowInfo.getPkts();
				}
			}
			tmp.put(dpid, swPktps);
		}

		// calculate sampling rate
		for (String dpid : mapForSwitchSamplingInfo.keySet()) {
			double swRate = tmp.get(dpid);
			if (total < 1e-6 || swRate < 10e-6) {
				mapForSwitchSamplingInfo.get(dpid).setInterval(0);
			} else {
				double rate = swRate / total;
				double avliableRate = rate * dpiMaxRate;
				if (avliableRate >= swRate) {
					mapForSwitchSamplingInfo.get(dpid).setInterval(0);
				} else {
					double samplingRate = avliableRate / swRate;
					long samplingTime = (long) (samplingRate * 1000);

					// logger.info("sampling rate: " + samplingRate + " sampling time: " +
					// samplingTime);

					mapForSwitchSamplingInfo.get(dpid).setInterval(((long) 1000 - samplingTime));
					mapForSwitchSamplingInfo.get(dpid).setSamplingTime(samplingTime);
				}
			}

			if (!workingSwitches.contains(dpid) && mapForSwitchSamplingInfo.get(dpid).getInterval() != 0) {
				reSampling(dpid, mapForSwitchSamplingInfo.get(dpid).getSamplingTime());
				workingSwitches.add(dpid);

			}
			// logger.info(dpid + "- samplingTime: " +
			// mapForSwitchSamplingInfo.get(dpid).getSamplingTime() + " Interval: "
			// + mapForSwitchSamplingInfo.get(dpid).getInterval());
		}
	}

	public void reSampling(String dpid, long samplingTime) {
		logger.info("Reschedule sampling... ");

		long stopSamplingTime = System.currentTimeMillis() + samplingTime;
		logger.info("set next stop sampling time: " + stopSamplingTime);

		JobDetail stopSamplingJob = JobBuilder.newJob(StopSamplingJob.class).usingJobData("dpid", dpid)
				.withIdentity("job-" + "-stop-" + dpid, "group").build();

		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger-" + "-stop-" + dpid, "group")
				.startAt(new Date(stopSamplingTime))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0)) // for
																													// only
																													// once
				.build();
		try {
			if (scheduler.isStarted()) {
				scheduler.scheduleJob(stopSamplingJob, trigger);
			}
		} catch (SchedulerException e) {
			logger.error(e.getMessage());
		}

	}

}
