package com.tank.infocollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tank.aaa.entity.Flow;
import com.tank.aaa.entity.FlowInfo;
import com.tank.aaa.entity.FlowStatics;
import com.tank.aaa.message.FlowRemovedMessage;
import com.tank.aaa.message.FlowStatsUpdateMessage;
import com.tank.aaa.message.IFlowMessageService;
import com.tank.aaa.switchselection.ISwitchSelectionService;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class SwitchSelectionInfoCollector implements IOFSwitchListener, IOFMessageListener, IFloodlightModule {
	protected IFloodlightProviderService floodlightProvider;
	private IFlowMessageService flowMessageService;
	protected static Logger logger;

	private final static long FLOW_DEFAULT_COLLECT_TIME = 5; /* meter stats default collect time 5 s */
	private final static TimeUnit FLOW_COLLECT_DEFAULE_TIME_UNIT = TimeUnit.SECONDS; /* defaule time unit */

	private final OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);

	private IThreadPoolService threadPoolService;

	private ScheduledFuture<?> flowStatsCollector;
	private static IOFSwitchService switchService;
	private ISwitchSelectionService switchSelectionService;

	@Override
	public String getName() {
		return "SwitchSelection module";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
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
		l.add(IFloodlightProviderService.class);
		l.add(IFlowMessageService.class);
		l.add(ISwitchSelectionService.class);
		l.add(IThreadPoolService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		flowMessageService = context.getServiceImpl(IFlowMessageService.class);
		switchSelectionService = context.getServiceImpl(ISwitchSelectionService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		logger = LoggerFactory.getLogger(SwitchSelectionInfoCollector.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
		floodlightProvider.addOFMessageListener(OFType.STATS_REPLY, this);
		switchService.addOFSwitchListener(this);

		flowStatsCollector = threadPoolService.getScheduledExecutor().scheduleAtFixedRate(new StatsCollector(), 0,
				FLOW_DEFAULT_COLLECT_TIME, FLOW_COLLECT_DEFAULE_TIME_UNIT);

		logger.info("Switch selection collection has start up!");
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		logger.info("Msg Type: " + msg.getType());
		switch (msg.getType()) {
		case FLOW_REMOVED:
			OFFlowRemoved oflr = (OFFlowRemoved) msg;
			Flow flow = null;
			if (oflr.getMatch().get(MatchField.IP_PROTO) == IpProtocol.TCP) {
				flow = new Flow(oflr.getMatch().get(MatchField.IPV4_SRC).getInt(),
						oflr.getMatch().get(MatchField.IPV4_DST).getInt(),
						oflr.getMatch().get(MatchField.IP_PROTO).getIpProtocolNumber(),
						oflr.getMatch().get(MatchField.TCP_SRC).getPort(),
						oflr.getMatch().get(MatchField.TCP_DST).getPort());
			} else if (oflr.getMatch().get(MatchField.IP_PROTO) == IpProtocol.UDP) {
				flow = new Flow(oflr.getMatch().get(MatchField.IPV4_SRC).getInt(),
						oflr.getMatch().get(MatchField.IPV4_DST).getInt(),
						oflr.getMatch().get(MatchField.IP_PROTO).getIpProtocolNumber(),
						oflr.getMatch().get(MatchField.UDP_SRC).getPort(),
						oflr.getMatch().get(MatchField.UDP_DST).getPort());
			}
			if (flow != null) {
				FlowStatics fss = new FlowStatics(flow, oflr.getDurationSec(), oflr.getDurationNsec(),
						oflr.getIdleTimeout(), oflr.getHardTimeout(), oflr.getPacketCount(), oflr.getByteCount());
				FlowRemovedMessage frMsg = new FlowRemovedMessage.Builder(fss).build();
				if (frMsg != null) {
					flowMessageService.publishMessage(frMsg);
					logger.info("Publish a flow removed message");
				}
			}
			// logger.info(oflr.toString());:w

			break;
		case STATS_REPLY:
			logger.info("recive Stats reply: " + msg.getType());
			OFStatsReply osr = (OFStatsReply) msg;
			if (osr.getStatsType().equals(OFStatsType.FLOW)) {
				logger.info("Recive Flow Stats reply");
				OFFlowStatsReply offsr = (OFFlowStatsReply) osr;
				FlowStatsUpdateMessage fsuMsg = new FlowStatsUpdateMessage.Builder().setFlowStatsReply(offsr).build();
				flowMessageService.publishMessage(fsuMsg);
			} else {
				logger.info("Recive stats NOT Flow Stats; " + osr.getStatsType());
			}
			break;
		default:
			break;
		}
		return Command.CONTINUE;
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		// TODO Auto-generated method stub

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

	/* period to collect meter stats of switchs */
	private class StatsCollector implements Runnable {

		@Override
		public void run() {
			logger.info("Sending to stats request...");
			/**
			 * synchronized block Collections synchronized Object is not implements
			 * iterator() synchronized
			 **/
			synchronized (switchSelectionService.getFlowInformation()) {
				logger.info("In syncronized block for Flows");
				Map<Flow, FlowInfo> flowInfos = switchSelectionService.getFlowInformation();

				for (Flow flow : flowInfos.keySet()) {
					logger.info("Sending to flow: " + flow);
					OFFlowStatsRequest ofFlowStatsRqst = buildFlowStatsRequest(flow);
					DatapathId dpid = flowInfos.get(flow).getPath().get(0);
					switchService.getSwitch(dpid).write(ofFlowStatsRqst);
					logger.info("Sended to sw: " + dpid.toString());
				}
			}

		}
	}

	/**
	 * build flow stats request
	 * 
	 * @return
	 */
	private OFFlowStatsRequest buildFlowStatsRequest(Flow flow) {
		Match match;
		if (flow.getIpProtocol() == IpProtocol.TCP.getIpProtocolNumber()) {
			match = factory.buildMatch().setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.IPV4_SRC, IPv4Address.of(flow.getSrcIp()))
					.setExact(MatchField.IPV4_DST, IPv4Address.of(flow.getDstIp()))
					.setExact(MatchField.IP_PROTO, IpProtocol.of(flow.getIpProtocol()))
					.setExact(MatchField.TCP_SRC, TransportPort.of(flow.getSrcPort()))
					.setExact(MatchField.TCP_DST, TransportPort.of(flow.getDstPort())).build();
		} else {
			match = factory.buildMatch().setExact(MatchField.ETH_TYPE, EthType.IPv4)
					.setExact(MatchField.IPV4_SRC, IPv4Address.of(flow.getSrcIp()))
					.setExact(MatchField.IPV4_DST, IPv4Address.of(flow.getDstIp()))
					.setExact(MatchField.IP_PROTO, IpProtocol.of(flow.getIpProtocol()))
					.setExact(MatchField.UDP_SRC, TransportPort.of(flow.getSrcPort()))
					.setExact(MatchField.UDP_DST, TransportPort.of(flow.getDstPort())).build();

		}
		return factory.buildFlowStatsRequest().setMatch(match).setOutPort(OFPort.ANY).setTableId(TableId.ALL)
				.setOutGroup(OFGroup.ANY).build();
	}
}
