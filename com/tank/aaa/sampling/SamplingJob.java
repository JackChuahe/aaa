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

import ch.qos.logback.classic.Logger;

public class SamplingJob implements org.quartz.Job {

	private long xid = -1;

	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {

		Object xidTmp = ctx.getJobDetail().getJobDataMap().get("xid");
		if (xidTmp != null) {
			xid = (Long) xidTmp;
		} else {
			HashBasedUniformSampling.logger.info("xid get is null, sampling job exit.");
			return;
		}
		String dpid = ctx.getJobDetail().getJobDataMap().getString("dpid");
		// sampling
		Map<String, SwitchSamplingInfo> map = HashBasedUniformSampling.getSwitchSamplingInfo();
		sendSamplingMsg(dpid, map);

		if (xid != HashBasedUniformSampling.xid) {
			HashBasedUniformSampling.logger.info("xid "+xid+" different, sampling job exit. now xid: "+HashBasedUniformSampling.xid);
			return;
		}

		if (map.get(dpid).getInterval() != 0) { // sampling all the time
			// set stop sampling task
			setStopSamplingTask(ctx.getScheduler(), dpid, map);
			HashBasedUniformSampling.workingSwitches.add(dpid);
		} else {
			HashBasedUniformSampling.workingSwitches.remove(dpid);
		}

	}

	/**
	 * 
	 * @param dpid
	 */
	public void sendSamplingMsg(String dpid, Map<String, SwitchSamplingInfo> map) {

		HashBasedUniformSampling.getCurrentSamplingSwitches().add(dpid);
		if (xid == HashBasedUniformSampling.xid) {
			HashBasedUniformSampling.getSwitchService().getSwitch(map.get(dpid).getDpid())
					.write(HashBasedUniformSampling.SamplingMsg);
			HashBasedUniformSampling.logger.info("Job: " + dpid + " sampling. At: " + System.currentTimeMillis());
		}
	}

	/**
	 * 
	 * @param scheduler
	 * @param dpid
	 */
	public void setStopSamplingTask(Scheduler scheduler, String dpid, Map<String, SwitchSamplingInfo> map) {

		long stopSamplingTime = System.currentTimeMillis() + map.get(dpid).getSamplingTime();
		HashBasedUniformSampling.logger.info("set " + dpid + " next stop sampling time: " + stopSamplingTime);

		JobDetail stopSamplingJob = JobBuilder.newJob(StopSamplingJob.class).usingJobData("dpid", dpid).usingJobData("xid",xid)
				.withIdentity("job-" + "-stop-" + dpid, "group").build();

		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger-" + "-stop-" + dpid, "group")
				.startAt(new Date(stopSamplingTime))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(0).withRepeatCount(0)) // for
																													// only
																													// once
				.build();
		try {
			scheduler.scheduleJob(stopSamplingJob, trigger);
		} catch (SchedulerException e) {
			HashBasedUniformSampling.logger.error(e.getMessage());
		}
	}

}
