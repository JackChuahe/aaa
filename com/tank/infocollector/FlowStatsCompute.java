package com.tank.infocollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tank.aaa.entity.Flow;
import com.tank.aaa.entity.FlowInfo;
import com.tank.aaa.entity.FlowStatics;
import com.tank.aaa.message.FlowMessage;
import com.tank.aaa.message.FlowMessageType;
import com.tank.aaa.message.FlowRemovedMessage;
import com.tank.aaa.message.FlowStatsUpdateMessage;
import com.tank.aaa.message.IFlowMessageListener;
import com.tank.aaa.message.IFlowMessageService;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class FlowStatsCompute implements IFlowStatsService, IFloodlightModule, IFlowMessageListener {

	public static final int FLOW_MAP_BASE_SIZE = 1000;
	public static final float FLOW_MAP_BASE_LOAD_FACTOR = (float) 1.0;

	private IFlowMessageService flowMessageService;
	protected static Logger logger;

	private static Map<Flow, FlowInfo> flowInfos = Collections
			.synchronizedMap(new HashMap<Flow, FlowInfo>(FLOW_MAP_BASE_SIZE, FLOW_MAP_BASE_LOAD_FACTOR));

	public Set<FlowStatsUpdateListener> listeners = new HashSet<FlowStatsUpdateListener>();

	@Override
	public Map<Flow, FlowInfo> getFlowStats() {
		return flowInfos;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFlowStatsService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IFlowStatsService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFlowMessageService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		flowMessageService = context.getServiceImpl(IFlowMessageService.class);
		logger = LoggerFactory.getLogger(FlowStatsCompute.class);

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		flowMessageService.addFlowMessageListener(FlowMessageType.FLOW_STATS_UPDATE, this);
		flowMessageService.addFlowMessageListener(FlowMessageType.FLOW_REMOVE, this);
		logger.info("Flow stats compute module start up!");
	}

	@Override
	public void messageRecive(List<FlowMessage> msgs) {
		if (msgs.size() == 0)
			return;
		logger.info("Flow Stats Compute Module reciced msgs: msg size: " + msgs.size() + " type: "
				+ msgs.get(0).getMessageType());

		switch (msgs.get(0).getMessageType()) {
		case FLOW_STATS_UPDATE:
			logger.info("In case FLOW_STATS_UPDATE");
			for (FlowMessage msg : msgs) {

				FlowStatsUpdateMessage fsuMsg = (FlowStatsUpdateMessage) msg;
				for (OFFlowStatsEntry entry : fsuMsg.getFlowStats().getEntries()) {
					Match match = entry.getMatch();
					Flow flow = null;
					logger.info(" " + msg);
					logger.info("Extract Flow Info...type: " + msg.getMessageType());
					if (match.get(MatchField.IP_PROTO).equals(IpProtocol.TCP)) {
						flow = new Flow(match.get(MatchField.IPV4_SRC).getInt(),
								match.get(MatchField.IPV4_DST).getInt(),
								match.get(MatchField.IP_PROTO).getIpProtocolNumber(),
								match.get(MatchField.TCP_SRC).getPort(), match.get(MatchField.TCP_DST).getPort());
					} else if (match.get(MatchField.IP_PROTO).equals(IpProtocol.UDP)) {
						flow = new Flow(match.get(MatchField.IPV4_SRC).getInt(),
								match.get(MatchField.IPV4_DST).getInt(),
								match.get(MatchField.IP_PROTO).getIpProtocolNumber(),
								match.get(MatchField.UDP_SRC).getPort(), match.get(MatchField.UDP_DST).getPort());
					} // get flow
					if (flow != null) {
						logger.info("compute flow speed");
						FlowInfo flowInfo = null;
						double duration = ((double) entry.getDurationSec())
								+ (double) entry.getDurationNsec() / (double) (10e9);

						logger.info("Duration: " + duration);

						if ((flowInfo = flowInfos.get(flow)) != null) {
							double durationTemp = duration - flowInfo.getDuration();
							double pkts = ((double) (entry.getPacketCount().getValue()
									- (double) flowInfo.getPacketCount())) / durationTemp;
							flowInfo.setPkts(pkts);

							logger.info("PKTS: " + pkts);
							double bps = ((double) entry.getByteCount().getValue() - (double) flowInfo.getByteCount())
									/ durationTemp;

							flowInfo.setBps(bps);
							logger.info("BPS: " + bps);

							flowInfo.setBytecount(entry.getByteCount().getValue());
							flowInfo.setPacketCount(entry.getPacketCount().getValue());
							flowInfo.setDuration(duration);
						} else {

							double pkts = ((double) entry.getPacketCount().getValue()) / duration;
							double bps = ((double) entry.getByteCount().getValue()) / duration;
							flowInfo = new FlowInfo();
							flowInfo.setPkts(pkts);
							flowInfo.setBps(bps);
							flowInfo.setDuration(duration);
							flowInfo.setPacketCount(entry.getPacketCount().getValue());
							flowInfo.setBytecount(entry.getByteCount().getValue());
							flowInfos.put(flow, flowInfo);
							logger.info("Flow Stats Add, flow Infos size: " + flowInfos.size());

						}
					}
				}
			}

			logger.info("Notify Flow Stats listener...");
			// notify flow stats update
			for (FlowStatsUpdateListener listener : listeners) {
				listener.flowStatsUpdate();
			}
			logger.info("Notify Flow Stats listener finished!");

			// fsuMsg.getFlowStats().getEntries().get(0)
			// logger.info("MessageRecived: Flow Stats Updated: " + flowInfos.toString());
			break;
		case FLOW_REMOVE:
			for (FlowMessage msg : msgs) {
				FlowRemovedMessage frMsg = (FlowRemovedMessage) msg;
				FlowStatics flowStatics = frMsg.getFlowStats();
				flowInfos.remove(flowStatics.getFlow());
				logger.info("Flow Removed, flow Infos : Flow Info Size: " + flowInfos.size());
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void addFlowStatsUpdateListener(FlowStatsUpdateListener listener) {
		listeners.add(listener);
	}
}
