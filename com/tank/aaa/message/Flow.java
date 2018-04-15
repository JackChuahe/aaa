package com.tank.aaa.message;

public class Flow {
	private static final int PRIME_1 = 3;
	private static final int PRIME_2 = 11;
	private static final int PRIME_3 = 23;
	private static final int PRIME_4 = 31;
	private static final int PRIME_5 = 17;

	private int srcIp;
	private int dstIp;
	private short ipProtocol; // udp // tcp // others
	private short srcPort;
	private short dstPort;

	public int getSrcIp() {
		return srcIp;
	}

	public void setSrcIp(int srcIp) {
		this.srcIp = srcIp;
	}

	public int getDstIp() {
		return dstIp;
	}

	public void setDstIp(int dstIp) {
		this.dstIp = dstIp;
	}

	public short getIpProtocol() {
		return ipProtocol;
	}

	public void setIpProtocol(short ipProtocol) {
		this.ipProtocol = ipProtocol;
	}

	public short getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(short srcPort) {
		this.srcPort = srcPort;
	}

	public short getDstPort() {
		return dstPort;
	}

	public void setDstPort(short dstPort) {
		this.dstPort = dstPort;
	}

	@Override
	public String toString() {
		return "flow: srcIp[" + srcIp + "]" + " dstIp[" + dstIp + "]" + " IpProtocol[" + ipProtocol + "]" + " srcPort["
				+ srcPort + "] dstPort[" + dstPort;
	}

	public Flow(int srcIp, int dstIp, short ipProtocol, short srcPort, short dstPort) {
		super();
		this.srcIp = srcIp;
		this.dstIp = dstIp;
		this.ipProtocol = ipProtocol;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
	}

	@Override
	public int hashCode() {
		return PRIME_1 * srcIp + PRIME_2 * dstIp + PRIME_3 * ipProtocol + PRIME_4 * srcPort + PRIME_5 * dstPort;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Flow) {
			Flow f = (Flow) obj;
			if (f.ipProtocol == ipProtocol && f.srcIp == srcIp && f.dstIp == dstIp && f.srcPort == srcPort
					&& f.dstPort == dstPort) {
				return true;
			}
			return false;
		}
		throw new ClassCastException(obj.getClass().getName() + " can not cast to " + this.getClass().getName());
	}
}
