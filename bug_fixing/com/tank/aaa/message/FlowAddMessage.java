package com.tank.aaa.message;

import com.tank.aaa.entity.Flow;

import net.floodlightcontroller.routing.Route;

public class FlowAddMessage extends FlowMessage {
	private Flow flow;
	private Route route;
	
	private FlowAddMessage(Flow flow, Route route) {
		this.flow = flow;
		this.route = route;
	}

	public Flow getFlow() {
		return flow;
	}

	public Route getRoute() {
		return route;
	}

	@Override
	public FlowMessageType getMessageType() {
		return FlowMessageType.FLOW_ADD;
	}

	public static class Builder {
		private Flow flow = null;
		private Route route = null;

		public Builder(Flow flow, Route route) {
			if (flow == null) {
				throw new RuntimeException("FlowAddMessage: flow is null!");
			}
			this.flow = flow;
			if (route == null) {
				throw new RuntimeException("FlowAddMessage: flow route is null!");
			}
			this.route = route;
		}

		public FlowAddMessage build() {
			return new FlowAddMessage(flow, route);
		}

	}
}
