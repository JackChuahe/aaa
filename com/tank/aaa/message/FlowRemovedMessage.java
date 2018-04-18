package com.tank.aaa.message;

import com.tank.aaa.entity.FlowStatics;

public class FlowRemovedMessage extends FlowMessage {
	private FlowStatics flowStats;

	public FlowStatics getFlowStats() {
		return flowStats;
	}

	private FlowRemovedMessage(FlowStatics flowStats) {
		this.flowStats = flowStats;
	}

	@Override
	public FlowMessageType getMessageType() {
		return FlowMessageType.FLOW_REMOVE;
	}

	public static class Builder {
		private FlowStatics flowStats;

		public Builder(FlowStatics flowS) {
			if (flowS != null && flowS.getFlow() != null) {
				this.flowStats = flowS;
			} else {
				throw new RuntimeException("FlowRemovedMessage: flow is null");
			}
		}

		public FlowRemovedMessage build() {
			return new FlowRemovedMessage(flowStats);
		}
	}

}
