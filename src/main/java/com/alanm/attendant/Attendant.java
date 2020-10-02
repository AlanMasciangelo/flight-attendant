package com.alanm.attendant;

import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.logging.Logger;

import jdk.jfr.consumer.RecordingStream;

public class Attendant {
	private static Logger LOGGER = Logger.getLogger(Attendant.class.getName());

	public static void premain(String agentArgs, Instrumentation inst) {
		takeoff();
	}

	public static void agentmain(String agentArgs, Instrumentation inst) {
		takeoff();
	}

	private static void takeoff() {
		LOGGER.info("Welcome aboard. Can I get you something to drink?");
		JfrEventConsumer consumer = JfrEventConsumer.createElasticConsumer();
		RecordingStream rs = new RecordingStream();
		Duration duration = Duration.ofSeconds(1);

		// Event configuration
		rs.enable("jdk.CPULoad").withPeriod(duration);
		rs.enable("jdk.JavaThreadStatistics").withPeriod(duration);
		rs.enable("jdk.JavaError").withoutThreshold();
		rs.enable("jdk.InitialEnvironmentVariable").withoutThreshold();
		rs.enable("jdk.InitialSystemProperty").withoutThreshold();

		rs.onEvent("jdk.CPULoad", consumer::onCPULoad);
		rs.onEvent("jdk.JavaThreadStatistics", consumer::onJavaThreadStatistics);
		rs.onEvent("jdk.JavaError", consumer::onJavaError);
		rs.onEvent("jdk.InitialEnvironmentVariable", consumer::onInitialEnvironmentVariable);
		rs.onEvent("jdk.InitialSystemProperty", consumer::onInitialSystemProperty);
		rs.startAsync();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				rs.close();
			}
		});

		// Some other interesting stats
//	      rs.enable("jdk.YoungGarbageCollection").withoutThreshold();
//	      rs.enable("jdk.OldGarbageCollection").withoutThreshold();
//	      rs.enable("jdk.GCHeapSummary").withPeriod(duration);
//	      rs.enable("jdk.PhysicalMemory").withPeriod(duration);
//	      rs.enable("jdk.GCConfiguration").withPeriod(duration);
//	      rs.enable("jdk.SafepointBegin");
//	      rs.enable("jdk.SafepointEnd");
//	      rs.enable("jdk.ObjectAllocationOutsideTLAB").withStackTrace();
//	      rs.enable("jdk.ObjectAllocationInNewTLAB").withStackTrace();
//	      rs.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(10)).withStackTrace();
//	      rs.enable("jdk.ClassLoadingStatistics").withPeriod(duration);
//	      rs.enable("jdk.Compilation").withoutThreshold();
//	      rs.enable("jdk.GCHeapConfiguration").withPeriod(duration);
//	      rs.enable("jdk.Flush").withoutThreshold();
	}
}