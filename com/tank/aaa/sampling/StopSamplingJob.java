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

	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {

		String dpid = ctx.getJobDetail().getJobDataMap().getString("dpid");
		Long xidTmp = HashBasedUniformSampling.switchMapXid.get(dpid);
		HashBasedUniformSampling.logger.info("Job: " + dpid + " Stop Sampling. At: " + System.currentTimeMillis());

		// sampling
		Map<String, SwitchSamplingInfo> map = HashBasedUniformSampling.getSwitchSamplingInfo();
		// set start sampling task
		if ((xidTmp == null || xidTmp != HashBasedUniformSampling.xid)
				&& !HashBasedUniformSampling.nextTimeSws.contains(map.get(dpid).getDpid())) {
			HashBasedUniformSampling.logger
					.error("sampling job: " + dpid + " have no xid or xid different. exit job! My xid: " + xidTmp
							+ " now Xid: " + HashBasedUniformSampling.xid);
			return;
		}

		sendStopSamplingMsg(dpid, map);
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

		JobDetail samplingJob = JobBuilder.newJob(SamplingJob.class).usingJobData("dpid", dpid)
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
