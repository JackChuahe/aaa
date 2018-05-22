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
		Object xidTmp = ctx.getJobDetail().getJobDataMap().get("xid");
		if (xidTmp != null) {
			xid = (Long) xidTmp;
		} else {
			HashBasedUniformSampling.logger.info("xid is get null, stop sampling exit.");
			return;
		}
		String dpid = ctx.getJobDetail().getJobDataMap().getString("dpid");
		HashBasedUniformSampling.logger.info("Job: " + dpid + " Stop Sampling. At: " + System.currentTimeMillis());

		if (xid != HashBasedUniformSampling.xid) {
			HashBasedUniformSampling.logger
					.info("xid +" + xid + " different, stop sampling exit. now xid: " + HashBasedUniformSampling.xid);
			return;
		}
		// sampling
		Map<String, SwitchSamplingInfo> map = HashBasedUniformSampling.getSwitchSamplingInfo();
		sendStopSamplingMsg(dpid, map);
		// set start sampling task
		setStartSamplingTask(ctx.getScheduler(), dpid, map);

	}

	/**
	 * 
	 * @param dpid
	 */
	public void sendStopSamplingMsg(String dpid, Map<String, SwitchSamplingInfo> map) {
		HashBasedUniformSampling.getSwitchService().getSwitch(map.get(dpid).getDpid())
				.write(HashBasedUniformSampling.DropMsg);

		HashBasedUniformSampling.getCurrentSamplingSwitches().remove(dpid);
	}

	/**
	 * 
	 * @param scheduler
	 * @param dpid
	 */
	public void setStartSamplingTask(Scheduler scheduler, String dpid, Map<String, SwitchSamplingInfo> map) {

		long startSamplingTime = System.currentTimeMillis() + map.get(dpid).getInterval();
		HashBasedUniformSampling.logger.info("set " + dpid + " next sampling time: " + startSamplingTime);

		JobDetail samplingJob = JobBuilder.newJob(SamplingJob.class).usingJobData("dpid", dpid).usingJobData("xid", xid)
				.withIdentity("job-" + "-sampling-" + dpid, "group").build();

		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger-" + "-sampling-" + dpid, "group")
				.startAt(new Date(startSamplingTime))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0)) // for
																													// only
																													// once
				.build();
		try {
			scheduler.scheduleJob(samplingJob, trigger);
		} catch (SchedulerException e) {
			HashBasedUniformSampling.logger.error(e.getMessage());
		}
	}
}
