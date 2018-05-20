package com.tank.aaa.message;

import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;

public class FlowStatsUpdateMessage extends FlowMessage {

	private OFFlowStatsReply ofFlowStatsReply;

	@Override
	public FlowMessageType getMessageType() {
		return FlowMessageType.FLOW_STATS_UPDATE;
	}

	private FlowStatsUpdateMessage(OFFlowStatsReply statsReply) {
		this.ofFlowStatsReply = statsReply;
	}

	public OFFlowStatsReply getFlowStats() {
		return ofFlowStatsReply;
	}

	public static class Builder {
		OFFlowStatsReply flowStatsReply;

		public Builder setFlowStatsReply(OFFlowStatsReply flowStatsReply) {
			this.flowStatsReply = flowStatsReply;
			return this;
		}

		public FlowStatsUpdateMessage build() {

			if (flowStatsReply != null)
				return new FlowStatsUpdateMessage(flowStatsReply);
			return null;
		}
	}
}
