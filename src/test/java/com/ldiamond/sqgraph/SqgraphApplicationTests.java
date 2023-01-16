package com.ldiamond.sqgraph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SqgraphApplicationTests {

	@Test
	void contextLoads() {
	}


	@Test
	void testGetMetricsListNeeded() {
		Config config = new Config();
		SQMetrics [] metricsArray = new SQMetrics [2];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");
		metricsArray [1] = new SQMetrics();
		metricsArray[0].setMetric("beta");

		List<String> results = SqgraphApplication.getMetricsListNeeded(config);
		assertEquals (2, results.size());
	}

	@Test
	void GetCommaSeparatedListOfMetrics() {
		List<String> metrics = new ArrayList<>();
		metrics.add ("alpha");
		metrics.add("beta");
		metrics.add("alpha");
		String output = SqgraphApplication.getCommaSeparatedListOfMetrics (metrics);
		assertEquals("alpha,beta", output);
	}


}
