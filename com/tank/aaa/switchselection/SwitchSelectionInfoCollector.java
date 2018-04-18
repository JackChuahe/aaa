package com.tank.aaa.switchselection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tank.aaa.entity.Flow;
import com.tank.aaa.entity.FlowStatics;
import com.tank.aaa.message.FlowMessageType;
import com.tank.aaa.message.FlowRemovedMessage;
import com.tank.aaa.message.IFlowMessageService;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class SwitchSelectionInfoCollector implements IOFMessageListener, IFloodlightModule {
	protected IFloodlightProviderService floodlightProvider;
	private IFlowMessageService flowMessageService;
	protected static Logger logger;

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
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		flowMessageService = context.getServiceImpl(IFlowMessageService.class);
		logger = LoggerFactory.getLogger(SwitchSelectionInfoCollector.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
		// floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		// floodlightProvider.addOFMessageListener(OFType.FLOW_MOD, this);
		logger.info("Switch selection has start up!");
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
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
					flowMessageService.pubMessage(frMsg);
					logger.info("Publish a flow removed message");
				}
			}
			 //logger.info(oflr.toString());
			break;
		// case PACKET_IN:
		// break;
		}
		return Command.CONTINUE;
	}
}
