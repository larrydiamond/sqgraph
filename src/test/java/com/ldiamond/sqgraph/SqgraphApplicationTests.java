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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.FileSystemAlreadyExistsException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
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
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [15];
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

		final Map<String, SyntheticMetric> syntheticMetrics = new HashMap<>();
		final SyntheticMetric unitTestSynthetic = new SyntheticMetric() {
			@Override public String getSyntheticName() {
				return "UnitTest";
			}

			@Override public List<String> getRealMetrics() {
				List<String> list = new ArrayList<>();
				list.add("violations");
				list.add("lines");
				return list;
			}

			@Override public double calculate(Map<String, Double> metrics) {
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

		final List<String> results = SqgraphApplication.getMetricsListNeeded(config, syntheticMetrics);
		assertEquals (5, results.size(), results.toString());
	}

	@Test
	void GetCommaSeparatedListOfMetrics() {
		final List<String> metrics = new ArrayList<>();
		metrics.add ("alpha");
		metrics.add("beta");
		metrics.add("alpha");
		final String output = SqgraphApplication.getCommaSeparatedListOfMetrics(metrics);
		assertEquals("alpha,beta", output);
	}

	@Test
	void testPopulatingSynthetics() {
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");

		final Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(4, synths.size());
	}

	@Test
	void testViolationsPerKLines() {
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");

		final Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		final SyntheticMetric sm = synths.get("ViolationsPerKLines");
		final Map<String, Double> metrics = new HashMap<>();
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
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");

		final Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		final SyntheticMetric sm = synths.get("CognitiveComplexityPerKLines");
		final Map<String, Double> metrics = new HashMap<>();
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
	void testBugsPlusSecurity() {
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("BugsPlusSecurity");

		final Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		final SyntheticMetric sm = synths.get("BugsPlusSecurity");
		final Map<String, Double> metrics = new HashMap<>();
		metrics.put("nothingofuse", 999.9);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put("bugs", 2.0);
		assertEquals(2.0, sm.calculate(metrics));

		metrics.put ("vulnerabilities", 5.0);
		assertEquals(7.0, sm.calculate(metrics));

		metrics.put ("security_hotspots", 8.0);
		assertEquals(15.0, sm.calculate(metrics));

		assertEquals(3, SqgraphApplication.getMetricsListNeeded(config, synths).size());
	}

	@Test
	void testBugsPlusSecurityPerKLines() {
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("BugsPlusSecurityPerKLines");

		final Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		final SyntheticMetric sm = synths.get("BugsPlusSecurityPerKLines");
		final Map<String, Double> metrics = new HashMap<>();
		metrics.put("nothingofuse", 999.9);
		assertTrue(Double.isNaN(sm.calculate(metrics)));

		metrics.put("ncloc", 10.0);
		assertEquals(0.0, sm.calculate(metrics));

		metrics.put("bugs", 2.0);
		assertEquals(200.0, sm.calculate(metrics));

		metrics.put ("vulnerabilities", 5.0);
		assertEquals(700.0, sm.calculate(metrics));

		metrics.put ("security_hotspots", 8.0);
		assertEquals(1500.0, sm.calculate(metrics));

		assertEquals(4, SqgraphApplication.getMetricsListNeeded(config, synths).size());
	}

	@Test
	void populateMetricsNoSynthetics() {
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [1];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");

		final Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(4, synths.size());
		assertNotNull(synths.get("ViolationsPerKLines"));
		assertNotNull(synths.get("CognitiveComplexityPerKLines"));
		assertNotNull(synths.get("BugsPlusSecurity"));
	}

	@Test
	void populateMetricsHasSyntheticPer() {
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [2];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");
		metricsArray [1] = new SQMetrics();
		metricsArray[1].setMetric("something__PER__otherthing");

		final Map<String, Double> metrics = new HashMap<>();
		metrics.put("something", 50.0);
		metrics.put("otherthing", 25.0);

		final Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(5, synths.size());
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
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [2];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");
		metricsArray [1] = new SQMetrics();
		metricsArray[1].setMetric("something__PER_K_otherthing");

		final Map<String, Double> metrics = new HashMap<>();
		metrics.put("something", 50.0);
		metrics.put("otherthing", 25.0);

		final Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(5, synths.size());
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
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [2];
		config.setMetrics(metricsArray);
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");
		metricsArray [1] = new SQMetrics();
		metricsArray[1].setMetric("something__PER_H_otherthing");

		final Map<String, Double> metrics = new HashMap<>();
		metrics.put("something", 50.0);
		metrics.put("otherthing", 25.0);

		final Map<String, SyntheticMetric> synths = SqgraphApplication.populateSynthetics(config);
		assertEquals(5, synths.size());
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

		final SearchHistory sh = new SearchHistory();
		final Paging paging = new Paging();
		paging.setTotal(2);
		final Measures[] measuresArray = new Measures[2];
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

		final ResponseEntity<SearchHistory> rsh = new ResponseEntity<>(sh, null, HttpStatus.OK);
		final HttpHeaders httpHeaders = new HttpHeaders();
		final HttpEntity<String> hes = new HttpEntity<>(httpHeaders);
		when (restTemplate.exchange("prefix/api/measures/search_history?from=blah&p=1&ps=999&component=blah&metrics=blah",HttpMethod.GET,hes,SearchHistory.class)).thenReturn(rsh);
		when (restTemplate.exchange("prefix/api/measures/search_history?from=blah&p=2&ps=999&component=blah&metrics=blah",HttpMethod.GET,hes,SearchHistory.class)).thenReturn(rsh);
		final AssembledSearchHistory ash = SqgraphApplication.getHistory(config, "blah", "blah", "blah", httpHeaders, restTemplate);

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

		final SearchHistory sh = new SearchHistory();
		final Paging paging = new Paging();
		paging.setTotal(0);
		sh.setPaging(paging);
		final Measures[] measuresArray = new Measures[0];
		sh.setMeasures(measuresArray);

		final ResponseEntity<SearchHistory> rsh = new ResponseEntity<>(sh, null, HttpStatus.OK);
		final HttpHeaders httpHeaders = new HttpHeaders();
		final HttpEntity<String> hes = new HttpEntity<>(httpHeaders);
		when (restTemplate.exchange("prefix/api/measures/search_history?from=blah&p=1&ps=999&component=blah&metrics=blah",HttpMethod.GET,hes,SearchHistory.class)).thenReturn(rsh);
		final AssembledSearchHistory ash = SqgraphApplication.getHistory(config, "blah", "blah", "blah", httpHeaders, restTemplate);

		assertEquals(0, ash.getMeasures().size());
	}

    private static Date parseRfc822(String s) throws ParseException {
        return new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH).parse(s);
    }

	@Test
	void testGetCommaSeparatedListOfApplications() {
		final String output = SqgraphApplication.getCommaSeparatedListOfMetrics(List.of("AppOne", "AppTwo", "AppThree"));
		assertEquals("AppOne,AppTwo,AppThree", output);
	}

	@Test 
	void testGetMetricsListNeededEmpty() {
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [0];
		config.setMetrics(metricsArray);

		final Map<String, SyntheticMetric> syntheticMetrics = new HashMap<>();

		final List<String> results = SqgraphApplication.getMetricsListNeeded(config, syntheticMetrics);
		assertEquals (0, results.size(), results.toString());
	}

	@Test 
	void testGetMetricsListNeededNoSynthetics() {
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [2];
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("alpha");
		metricsArray [1] = new SQMetrics();
		metricsArray[1].setMetric("beta");
		config.setMetrics(metricsArray);

		final Map<String, SyntheticMetric> syntheticMetrics = SqgraphApplication.populateSynthetics(config);

		final List<String> results = SqgraphApplication.getMetricsListNeeded(config, syntheticMetrics);
		assertEquals (2, results.size(), results.toString());
	}

	@Test 
	void testGetMetricsListNeededWithSynthetics() {
		final Config config = new Config();
		final SQMetrics [] metricsArray = new SQMetrics [2];
		metricsArray [0] = new SQMetrics();
		metricsArray[0].setMetric("ViolationsPerKLines");
		metricsArray [1] = new SQMetrics();
		metricsArray[1].setMetric("CognitiveComplexityPerKLines");
		config.setMetrics(metricsArray);

		final Map<String, SyntheticMetric> syntheticMetrics = SqgraphApplication.populateSynthetics(config);

		final List<String> results = SqgraphApplication.getMetricsListNeeded(config, syntheticMetrics);
		assertEquals (3, results.size(), results.toString());
	}

	@Test
	void testValidateSonarTokenGood() {
		final Config config = new Config();
		config.setUrl("someurl");
		final ValidationResult validationResult = new ValidationResult();
		validationResult.setValid(true);
		final ResponseEntity<ValidationResult> response = new ResponseEntity<>(validationResult, HttpStatus.OK);
		final RestTemplate localRestTemplate = mock(RestTemplate.class);
		when(localRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(ValidationResult.class))).thenReturn(response);
		final boolean b = new SqgraphApplication().validateSonarToken(config, new HttpHeaders(), localRestTemplate);
		assertTrue(b);
	}

	@Test
	void testValidateSonarTokenBad() {
		final Config config = new Config();
		config.setUrl("someurl");
		final ValidationResult validationResult = new ValidationResult();
		validationResult.setValid(false);
		final ResponseEntity<ValidationResult> response = new ResponseEntity<>(validationResult, HttpStatus.OK);
		final RestTemplate localRestTemplate = mock(RestTemplate.class);
		when(localRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(ValidationResult.class))).thenReturn(response);
		final boolean b = new SqgraphApplication().validateSonarToken(config, new HttpHeaders(), localRestTemplate);
		assertFalse(b);
	}

	@Test
	void testValidateSonarTokenNull() {
		final Config config = new Config();
		config.setUrl("someurl");
		final ResponseEntity<ValidationResult> response = new ResponseEntity<>(null, HttpStatus.OK);
		final RestTemplate localRestTemplate = mock(RestTemplate.class);
		when(localRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(ValidationResult.class))).thenReturn(response);
		final boolean b = new SqgraphApplication().validateSonarToken(config, new HttpHeaders(), localRestTemplate);
		assertFalse(b);
	}


    @Test
    void testGetCommaSeparatedListOfMetrics_removesDuplicates_preservesOrder() {
		final List<String> input = Arrays.asList("a", "b", "a", "c", "b");
		final String result = SqgraphApplication.getCommaSeparatedListOfMetrics(input);
        assertEquals("a,b,c", result);
    }

    @Test
    void testViolationsPerKLines_calculation() {
		final Map<String, Double> metrics = new HashMap<>();
        metrics.put("violations", 5.0);
        metrics.put("ncloc", 2500.0);
		final double value = SqgraphApplication.ViolationsPerKLines.calculate(metrics);
        assertEquals(2.0, value, 0.0001);
    }

    @Test
    void testBugsPlusSecurity_calculation_withMissingValues() {
		final Map<String, Double> metrics = new HashMap<>();
        metrics.put("bugs", 1.0);
        metrics.put("vulnerabilities", 2.0);
		// security_hotspots absent
		final double value = SqgraphApplication.bugsPlusSecurity.calculate(metrics);
        assertEquals(3.0, value, 0.0001);
    }

    @Test
    void testGetMetricsListNeeded_handlesSyntheticAndRealMetrics() {
		// mock Config and SQMetrics
		final Config cfg = mock(Config.class);
		final SQMetrics sqm1 = mock(SQMetrics.class);
        when(sqm1.getMetric()).thenReturn("metricA");
		final SQMetrics sqm2 = mock(SQMetrics.class);
        when(sqm2.getMetric()).thenReturn("metricSynthetic");

        when(cfg.getMetrics()).thenReturn(new SQMetrics[] { sqm1, sqm2 });

		final SyntheticMetric sm = new SyntheticMetric() {
			@Override public String getSyntheticName() {
				return "metricSynthetic";
			}

			@Override public List<String> getRealMetrics() {
				return Arrays.asList("real1", "real2");
			}

			@Override public double calculate(Map<String, Double> metrics) {
				return 0;
			}
		};

		final Map<String, SyntheticMetric> synthetics = new HashMap<>();
        synthetics.put("metricSynthetic", sm);

		final List<String> needed = SqgraphApplication.getMetricsListNeeded(cfg, synthetics);
        assertTrue(needed.contains("metricA"));
        assertTrue(needed.contains("real1"));
        assertTrue(needed.contains("real2"));
        // ensure no duplication
        assertEquals(3, needed.size());
    }

    @Test
    void testPopulateSynthetics_createsSynthetic_for_Per_K_and_builtinsPresent() {
		final Config cfg = mock(Config.class);

		final SQMetrics sqmA = mock(SQMetrics.class);
        when(sqmA.getMetric()).thenReturn("a__PER_K_b"); // should create synthetic with multiplier 1000

        when(cfg.getMetrics()).thenReturn(new SQMetrics[] { sqmA });

		final Map<String, SyntheticMetric> synthetics = SqgraphApplication.populateSynthetics(cfg);
        assertNotNull(synthetics);
        // builtin synthetic metrics should be present
        assertTrue(synthetics.containsKey("ViolationsPerKLines"));
        assertTrue(synthetics.containsKey("CognitiveComplexityPerKLines"));
        assertTrue(synthetics.containsKey("BugsPlusSecurity"));
        // our generated synthetic
        assertTrue(synthetics.containsKey("a__PER_K_b"));
		final SyntheticMetric generated = synthetics.get("a__PER_K_b");
		final Map<String, Double> metrics = new HashMap<>();
        metrics.put("a", 2.0);
        metrics.put("b", 4.0);
		final double v = generated.calculate(metrics); // (1000 * 2) / 4 = 500
        assertEquals(500.0, v, 0.0001);
    }

    @Test
    void testValidateSonarToken_returnsTrueWhenValid() {
		// mocks
		final Config cfg = mock(Config.class);
        when(cfg.getUrl()).thenReturn("http://sonar.example");
		final RestTemplate rest = mock(RestTemplate.class);

		final ValidationResult vr = mock(ValidationResult.class);
        when(vr.isValid()).thenReturn(true);

		final ResponseEntity<ValidationResult> resp = new ResponseEntity<>(vr, HttpStatus.OK);
        when(rest.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(ValidationResult.class)))
            .thenReturn(resp);

		final SqgraphApplication app = new SqgraphApplication();
		final boolean ok = app.validateSonarToken(cfg, new HttpHeaders(), rest);
        assertTrue(ok);
    }

    @Test
    void testValidateSonarToken_returnsFalseOnException() {
		final Config cfg = mock(Config.class);
        when(cfg.getUrl()).thenReturn("http://sonar.example");
		final RestTemplate rest = mock(RestTemplate.class);

        when(rest.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(ValidationResult.class)))
            .thenThrow(new FileSystemAlreadyExistsException("boom"));

		final SqgraphApplication app = new SqgraphApplication();
		final boolean ok = app.validateSonarToken(cfg, new HttpHeaders(), rest);
        assertFalse(ok);
    }

	@Test
	void testBuildTitleLookup() {
		Config config = new Config();
		List<Application> apps = new ArrayList<>();
		Application app1 = new Application();
		app1.setKey("appKey1");
		app1.setTitle("Application One");
		apps.add(app1);
		Application app2 = new Application();
		app2.setKey("appKey2");
		app2.setTitle("Application Two");
		apps.add(app2);
		config.setExpandedApplications(apps);
		Map<String, String> titleLookup = SqgraphApplication.buildTitleLookup(config);
		assertEquals(2, titleLookup.size());
		assertEquals("Application One", titleLookup.get("appKey1"));
		assertEquals("Application Two", titleLookup.get("appKey2"));
	}

	@Test
	void testBuildAuthHeaders() {
		HttpHeaders headers = SqgraphApplication.buildAuthHeaders("myToken");
		assertNotNull(headers);
		assertTrue(headers.containsKey("Authorization"));
		assertEquals("Basic bXlUb2tlbjo=", headers.getFirst("Authorization"));
	}
}
