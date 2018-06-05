package com.tank.aaa.sampling;

import java.util.Date;
import java.util.Map;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

public class StopSamplingJob implements org.quartz.Job {
	private long xid = -1;

	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {

		String dpid = ctx.getJobDetail().getJobDataMap().getString("dpid");
		Long xidTmp = HashBasedUniformSampling.switchMapXid.get(dpid);
		HashBasedUniformSampling.logger.info("Job: " + dpid + " Stop Sampling. At: " + System.currentTimeMillis());

		// sampling
		Map<String, SwitchSamplingInfo> map = HashBasedUniformSampling.getSwitchSamplingInfo();
		SwitchSamplingInfo samplingInfo = map.get(dpid);
		// set start sampling task
		if ((xidTmp == null || xidTmp != HashBasedUniformSampling.xid)
				&& (samplingInfo == null || !HashBasedUniformSampling.nextTimeSws.contains(samplingInfo.getDpid()))) {
			HashBasedUniformSampling.logger.error("sampling job: " + dpid
					+ " have no xid or xid different or sampling information. exit job! My xid: " + xidTmp
					+ " now Xid: " + HashBasedUniformSampling.xid);
			return;
		} else {
			xid = xidTmp;
		}

		boolean isSendOk = sendStopSamplingMsg(dpid, map);
		if (!isSendOk) {
			HashBasedUniformSampling.logger.info("Stop sampling message not send ok . return. exit job");
			return;
		}

		samplingInfo = map.get(dpid);
		if (xid == HashBasedUniformSampling.xid
				|| (samplingInfo != null && HashBasedUniformSampling.nextTimeSws.contains(samplingInfo.getDpid()))) {
			setStartSamplingTask(ctx.getScheduler(), dpid, samplingInfo,map);
		} else {
			HashBasedUniformSampling.logger.info("xid different. not start up sampling job. exit");
		}

	}

	/**
	 * 
	 * @param dpid
	 */
	public boolean sendStopSamplingMsg(String dpid, Map<String, SwitchSamplingInfo> map) {
		SwitchSamplingInfo samplingInfo = map.get(dpid);

		if (xid == HashBasedUniformSampling.xid
				|| (samplingInfo != null && HashBasedUniformSampling.nextTimeSws.contains(samplingInfo.getDpid()))) {

			HashBasedUniformSampling.getSwitchService().getSwitch(map.get(dpid).getDpid())
					.write(HashBasedUniformSampling.DropMsg);

			HashBasedUniformSampling.getCurrentSamplingSwitches().remove(dpid);
			return true;
		}
		return false;

	}

	/**
	 * 
	 * @param scheduler
	 * @param dpid
	 */
	public void setStartSamplingTask(Scheduler scheduler, String dpid, SwitchSamplingInfo samplingInfo,
			Map<String, SwitchSamplingInfo> map) {

		long startSamplingTime = System.currentTimeMillis() + samplingInfo.getInterval();
		HashBasedUniformSampling.logger.info("set " + dpid + " next sampling time: " + startSamplingTime);

		JobDetail samplingJob = JobBuilder.newJob(SamplingJob.class).usingJobData("dpid", dpid)
				.withIdentity("job-" + "-sampling-" + dpid, "group").build();

		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger-" + "-sampling-" + dpid, "group")
				.startAt(new Date(startSamplingTime))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0)) // for
																													// only
																													// once
				.build();
		try {

			samplingInfo = map.get(dpid);
			if (xid != HashBasedUniformSampling.xid && (samplingInfo == null
					|| !HashBasedUniformSampling.nextTimeSws.contains(samplingInfo.getDpid()))) {
				HashBasedUniformSampling.logger.error("stop job: " + dpid
						+ " have no xid or xid different or sampling information. stop start sampling job. exit job! My xid: "
						+ xid + " now Xid: " + HashBasedUniformSampling.xid);

				return;
			}
			scheduler.scheduleJob(samplingJob, trigger);
		} catch (SchedulerException e) {
			HashBasedUniformSampling.logger.error(e.getMessage());
		}
	}
}
