// Copyright Larry Diamond 2023 All Rights Reserved
package com.ldiamond.sqgraph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SqgraphApplicationTests {

	@Test
	void contextLoads() {
	}


	@Test
	void testGetMetricsListNeeded() {
		Config config = new Config();
		SQMetrics [] metricsArray = new SQMetrics [15];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");
		metricsArray [1] = new SQMetrics();
		metricsArray[1].setMetric("beta");
		metricsArray [2] = new SQMetrics();
		metricsArray[2].setMetric("beta");
		metricsArray [3] = new SQMetrics();
		metricsArray[3].setMetric("lines");
		metricsArray [4] = new SQMetrics();
		metricsArray[4].setMetric("UnitTest");
		metricsArray [5] = new SQMetrics();
		metricsArray[5].setMetric("beta");
		metricsArray [6] = new SQMetrics();
		metricsArray[6].setMetric("beta");
		metricsArray [7] = new SQMetrics();
		metricsArray[7].setMetric("beta");
		metricsArray [8] = new SQMetrics();
		metricsArray[8].setMetric("beta");
		metricsArray [9] = new SQMetrics();
		metricsArray[9].setMetric("beta");
		metricsArray [10] = new SQMetrics();
		metricsArray[10].setMetric("UnitTest");
		metricsArray [11] = new SQMetrics();
		metricsArray[11].setMetric("UnitTest");
		metricsArray [12] = new SQMetrics();
		metricsArray[12].setMetric("UnitTest");
		metricsArray [13] = new SQMetrics();
		metricsArray[13].setMetric("UnitTest");
		metricsArray [14] = new SQMetrics();
		metricsArray[14].setMetric("gamma");

		Map<String,SyntheticMetric> syntheticMetrics = new HashMap<>();
		SyntheticMetric unitTestSynthetic = new SyntheticMetric() {
			@Override public String getSyntheicName() { return "UnitTest";}
			@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add ("violations");  list.add("lines");  return list;}
			@Override public double calculate(Map<String,Double> metrics) {
				double lines = 0;
				Double lineInput = metrics.get("lines");
				if (lineInput != null) lines = lineInput;
				double violations = 0;
				Double violationsInput = metrics.get("violations");
				if (violationsInput != null) violations = violationsInput;
				if ((lines == 0) || (violations == 0)) return 0.0;
				return violations / lines;
			}
		};

		syntheticMetrics.put(unitTestSynthetic.getSyntheicName(), unitTestSynthetic);

		List<String> results = SqgraphApplication.getMetricsListNeeded(config,syntheticMetrics);
		assertEquals (5, results.size(), results.toString());
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

	@Test
	void testPopulatingSynthetics() {
		Config config = new Config();
		SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");

		Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(2, synths.size());
	}

	@Test
	void testViolationsPerKLines() {
		Config config = new Config();
		SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");

		Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		SyntheticMetric sm = synths.get ("ViolationsPerKLines");
		Map<String,Double> metrics = new HashMap<>();
		metrics.put("nothingofuse", 999.9);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put("lines", 0.0);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put ("lines", 500.0);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put ("violations", 500.0);
		assertEquals(1000.0, sm.calculate(metrics));
	}

	@Test
	void testCognitiveComplexityPerKLines() {
		Config config = new Config();
		SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");

		Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		SyntheticMetric sm = synths.get ("CognitiveComplexityPerKLines");
		Map<String,Double> metrics = new HashMap<>();
		metrics.put("nothingofuse", 999.9);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put("lines", 0.0);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put ("lines", 500.0);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put ("cognitive_complexity", 500.0);
		assertEquals(1000.0, sm.calculate(metrics));
	}

}
