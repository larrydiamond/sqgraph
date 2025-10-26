/**
 *    Copyright (C) 2023-present Larry Diamond, All Rights Reserved.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    license linked below for more details.
 *
 *    For license terms, see <https://github.com/larrydiamond/sqgraph/blob/main/LICENSE.md>.
 **/
package com.ldiamond.sqgraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)     
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
			@Override public String getSyntheticName() { return "UnitTest";}
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

		syntheticMetrics.put(unitTestSynthetic.getSyntheticName(), unitTestSynthetic);

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
		assertEquals(3, synths.size());
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

		metrics.put("ncloc", 0.0);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put ("ncloc", 500.0);
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

		metrics.put("ncloc", 0.0);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put ("ncloc", 500.0);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put ("cognitive_complexity", 500.0);
		assertEquals(1000.0, sm.calculate(metrics));
	}

	@Test
	void populateMetricsNoSynthetics() {
		Config config = new Config();
		SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");

		Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(3, synths.size());
		assertNotNull(synths.get("ViolationsPerKLines"));
		assertNotNull(synths.get("CognitiveComplexityPerKLines"));
		assertNotNull(synths.get("BugsPlusSecurity"));
	}

	@Test
	void populateMetricsHasSyntheticPer() {
		Config config = new Config();
		SQMetrics [] metricsArray = new SQMetrics [2];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");
		metricsArray [1] = new SQMetrics();
		metricsArray[1].setMetric("something__PER__otherthing");

		Map<String,Double> metrics = new HashMap<>();
		metrics.put("something", 50.0);
		metrics.put("otherthing", 25.0);

		Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(4, synths.size());
		assertNotNull(synths.get("ViolationsPerKLines"));
		assertNotNull(synths.get("CognitiveComplexityPerKLines"));
		assertNotNull(synths.get("BugsPlusSecurity"));
		assertNotNull(synths.get("something__PER__otherthing"));
		assertEquals("something", synths.get("something__PER__otherthing").getRealMetrics().getFirst());
		assertEquals("otherthing", synths.get("something__PER__otherthing").getRealMetrics().get(1));
		assertEquals(2.0, synths.get("something__PER__otherthing").calculate(metrics), 0);
	}

	@Test
	void populateMetricsHasSyntheticPerK() {
		Config config = new Config();
		SQMetrics [] metricsArray = new SQMetrics [2];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");
		metricsArray [1] = new SQMetrics();
		metricsArray[1].setMetric("something__PER_K_otherthing");

		Map<String,Double> metrics = new HashMap<>();
		metrics.put("something", 50.0);
		metrics.put("otherthing", 25.0);

		Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(4, synths.size());
		assertNotNull(synths.get("ViolationsPerKLines"));
		assertNotNull(synths.get("CognitiveComplexityPerKLines"));
		assertNotNull(synths.get("BugsPlusSecurity"));
		assertNotNull(synths.get("something__PER_K_otherthing"));
		assertEquals("something", synths.get("something__PER_K_otherthing").getRealMetrics().getFirst());
		assertEquals("otherthing", synths.get("something__PER_K_otherthing").getRealMetrics().get(1));
		assertEquals(2000.0, synths.get("something__PER_K_otherthing").calculate(metrics), 0);
	}

	@Test
	void populateMetricsHasSyntheticPerH() {
		Config config = new Config();
		SQMetrics [] metricsArray = new SQMetrics [2];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");
		metricsArray [1] = new SQMetrics();
		metricsArray[1].setMetric("something__PER_H_otherthing");

		Map<String,Double> metrics = new HashMap<>();
		metrics.put("something", 50.0);
		metrics.put("otherthing", 25.0);

		Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(4, synths.size());
		assertNotNull(synths.get("ViolationsPerKLines"));
		assertNotNull(synths.get("CognitiveComplexityPerKLines"));
		assertNotNull(synths.get("BugsPlusSecurity"));
		assertNotNull(synths.get("something__PER_H_otherthing"));
		assertEquals("something", synths.get("something__PER_H_otherthing").getRealMetrics().getFirst());
		assertEquals("otherthing", synths.get("something__PER_H_otherthing").getRealMetrics().get(1));
		assertEquals(200.0, synths.get("something__PER_H_otherthing").calculate(metrics), 0);
	}

	@Mock
	RestTemplate restTemplate;

	@Test
	void getHistory() throws ParseException {
		final Config config = new Config();
		config.setUrl("prefix");

		SearchHistory sh = new SearchHistory();
		Paging paging = new Paging();
		paging.setTotal(2);
		Measures[] measuresArray = new Measures[2];
		sh.setMeasures(measuresArray);
		measuresArray [0] = new Measures();
		measuresArray [0].setMetric("first");
		measuresArray [0].setHistory(new History[1]);
		measuresArray [0].history[0] = new History();
		measuresArray [0].history[0].setDate(parseRfc822("Tue, 1 Aug 1995 13:30:00 GMT"));
		measuresArray [0].history[0].setValue(1.0);
		
		measuresArray [1] = new Measures();
		measuresArray [1].setMetric("second");
		measuresArray [1].setHistory(new History[1]);
		measuresArray [1].history[0] = new History();
		measuresArray [1].history[0].setDate(parseRfc822("Wed, 2 Aug 1995 13:30:00 GMT"));
		measuresArray [1].history[0].setValue(2.0);
		
		sh.setPaging(paging);

		ResponseEntity<SearchHistory> rsh = new ResponseEntity<>(sh, null, HttpStatus.OK);
		HttpHeaders httpHeaders = new HttpHeaders();
		HttpEntity<String> hes = new HttpEntity<>(httpHeaders);
		when (restTemplate.exchange("prefix/api/measures/search_history?from=blah&p=1&ps=999&component=blah&metrics=blah",HttpMethod.GET,hes,SearchHistory.class)).thenReturn(rsh);
		when (restTemplate.exchange("prefix/api/measures/search_history?from=blah&p=2&ps=999&component=blah&metrics=blah",HttpMethod.GET,hes,SearchHistory.class)).thenReturn(rsh);
		AssembledSearchHistory ash = SqgraphApplication.getHistory(config, "blah", "blah", "blah", httpHeaders, restTemplate);

		assertEquals(4, ash.getMeasures().size());
		assertEquals("first", ash.getMeasures().getFirst().getMetric());
		assertEquals("second", ash.getMeasures().get(1).getMetric());
		assertEquals("first", ash.getMeasures().get(2).getMetric());
		assertEquals("second", ash.getMeasures().get(3).getMetric());
		assertEquals(1, ash.getMeasures().getFirst().history.length);
		assertEquals(1, ash.getMeasures().get(1).history.length);
		assertEquals(1, ash.getMeasures().get(2).history.length);
		assertEquals(1, ash.getMeasures().get(3).history.length);
	}

	@Test
	void getHistoryEmpty() {
	
		final Config config = new Config();
		config.setUrl("prefix");

		SearchHistory sh = new SearchHistory();
		Paging paging = new Paging();
		paging.setTotal(0);
		sh.setPaging(paging);
		Measures[] measuresArray = new Measures[0];
		sh.setMeasures(measuresArray);

		ResponseEntity<SearchHistory> rsh = new ResponseEntity<>(sh, null, HttpStatus.OK);
		HttpHeaders httpHeaders = new HttpHeaders();
		HttpEntity<String> hes = new HttpEntity<>(httpHeaders);
		when (restTemplate.exchange("prefix/api/measures/search_history?from=blah&p=1&ps=999&component=blah&metrics=blah",HttpMethod.GET,hes,SearchHistory.class)).thenReturn(rsh);
		AssembledSearchHistory ash = SqgraphApplication.getHistory(config, "blah", "blah", "blah", httpHeaders, restTemplate);

		assertEquals(0, ash.getMeasures().size());
	}

    private static Date parseRfc822(String s) throws ParseException {
        return new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH).parse(s);
    }
}
