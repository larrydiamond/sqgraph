// Copyright Larry Diamond 2023 All Rights Reserved
package com.ldiamond.sqgraph;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.awt.image.BufferedImage;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.apache.commons.lang3.time.DateUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashBasedTable;

@SpringBootApplication
public class SqgraphApplication {
	static String login = null;
	static String filename = null;
	public static final String standardDecimalFormat = "###,###,###.###";
	public static final DecimalFormat standardDecimalFormatter = new DecimalFormat (standardDecimalFormat);

	public static void main(String[] args) {
		login = System.getenv("SONARLOGIN");
		if (login == null) {
			System.out.println ("Please create a user token in your SonarQube server and set that token in your environment as SONARLOGIN");
			System.exit (1);
		}

		if (args.length < 1) {
			System.out.println ("Please specify the config file to use on the command line");
			System.exit (1);		
		} 

		if (args [0] == null) {
			System.out.println ("Please specify the config file to use on the command line");
			System.exit (1);
		}
		filename = args [0];

		SpringApplication.run(SqgraphApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {

		Config config = null;
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			config = objectMapper.readValue(new File(filename), Config.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}	

		if (config == null) {
			System.out.println ("Configuration required");
			return null;
		}

		Map<String,SyntheticMetric> syntheticMetrics = populateSynthetics(config);

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		String base64 = "Basic " + new String (Base64.getEncoder().encodeToString (new String (login + ":").getBytes()));
		headers.set ("Authorization", base64);

		try {
			final String uri = config.getUrl() + "/api/authentication/validate";
			ResponseEntity<ValidationResult> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), ValidationResult.class);
			ValidationResult result = response.getBody();
			if ((result != null) && (!result.isValid())) {
				System.out.println ("SonarQube login token was not valid");
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		Map<String, String> titleLookup = new HashMap<>();
		final SimpleDateFormat sdfsq = new SimpleDateFormat("yyyy-MM-dd");

		Map<String, SearchHistory> rawMetrics = new HashMap<>();
		for (Application app : config.getApplications()) {
			String key = app.getKey();
			titleLookup.put(key, app.getTitle());
			try {
				List<String> metricsToQuery = getMetricsListNeeded(config,syntheticMetrics);
				String metrics = getCommaSeparatedListOfMetrics (metricsToQuery);
				
				Date startDate = new Date();
				startDate = DateUtils.addDays (startDate, (-1 * config.getMaxReportHistory()));
				Date sqDate = getUTCDate (startDate);
				String sdfsqString = sdfsq.format (sqDate);

				final String uri = config.getUrl() + "/api/measures/search_history?from="+sdfsqString+"&ps=999&component=" + key + "&metrics=" + metrics;
				ResponseEntity<SearchHistory> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), SearchHistory.class);
				SearchHistory result = response.getBody();
				rawMetrics.put (key, result);
/*
				ResponseEntity<String> responseString = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), String.class);
				String resultString = responseString.getBody();
				System.out.println ("Resultstring for " + metrics + " = " + resultString);
/* */
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}


		/*
	
		final String uri = config.getUrl() + "/api/metrics/search?ps=499";
		ResponseEntity<MetricsResults> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), MetricsResults.class);
		MetricsResults metricsResults = response.getBody();
		System.out.println ("Metrics = " + metricsResults.toString());
		for (Metric m : metricsResults.metrics) {
			System.out.println (m.toString());
		}

		/* */

		HashBasedTable<String,String,Double> dashboardData = HashBasedTable.create(config.getMetrics().length, 100);

		GraphOutput.outputGraphs(config, rawMetrics, dashboardData, titleLookup, syntheticMetrics);

		BufferedImage bi = DashboardOutput.outputDashboard(dashboardData, config);

		if (config.getPdf() != null) {
			PDFOutput.createPDF (config);
// not ready for prime time..... yet			PDFOutput.addTextDashboard (dashboardData, config);
			if (bi != null)
				PDFOutput.addDashboard (bi);
			PDFOutput.addGraphs(config);
			PDFOutput.closePDF();
		}

		System.out.println ("Successful completion.");
		return null;
	}

	public static List<String> getMetricsListNeeded (final Config config, final Map<String,SyntheticMetric> synthetics) {
		List<String> results = new ArrayList<>();
		for (SQMetrics sqm : config.getMetrics()) {
			String metric = sqm.getMetric();
			SyntheticMetric sm = synthetics.get(metric);
			if (sm == null) {
				if (!results.contains(metric)) results.add(metric);
			} else {
				for (String real : sm.getRealMetrics()) {
					if (!results.contains(real)) results.add(real);
				}
			}
		}
		return results;
	}

	public static String getCommaSeparatedListOfMetrics (final List<String> metrics) {
		StringBuilder output = new StringBuilder();
		boolean comma = false;
		List<String> alreadyAdded = new ArrayList<>();
		for (String metric : metrics) {
			if (!alreadyAdded.contains(metric)) {
				alreadyAdded.add(metric);
				if (!comma) {
					comma = true;
				} else {
					output.append (",");
				}
				output.append (metric);
			}
		}
		return output.toString();
	}

	public static Map<String,SyntheticMetric> populateSynthetics (final Config config) {
		Map<String,SyntheticMetric> syntheticMetrics = new HashMap<>();

		for (SQMetrics sqm : config.getMetrics()) {
			int offset = sqm.getMetric().indexOf("__PER__");
			if (offset != -1) {
				String prefix = sqm.getMetric().substring(0, offset);
				String suffix = sqm.getMetric().substring(offset + 7);

//				System.out.println ("Made synthetic " + sqm.getMetric() + " from " + prefix + " and " + suffix);

				SyntheticMetric generatedMetric = new SyntheticMetric() {
					@Override public String getSyntheicName() { return sqm.getMetric();}
					@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add (prefix);  list.add(suffix);  return list;}
					@Override public double calculate(Map<String,Double> metrics) {
						double denominator = 0;
						Double denominatorInput = metrics.get(suffix);
						if (denominatorInput != null) denominator = denominatorInput;
						double numerator = 0;
						Double numeratorInput = metrics.get(prefix);
						if (numeratorInput != null) numerator = numeratorInput;
						if ((denominator == 0) || (numerator == 0)) return 0.0;
						return numerator / denominator;
					}
				};
				syntheticMetrics.put(sqm.getMetric(), generatedMetric);
			}

			offset = sqm.getMetric().indexOf("__PER_K_");
			if (offset != -1) {
				String prefix = sqm.getMetric().substring(0, offset);
				String suffix = sqm.getMetric().substring(offset + 8);

//				System.out.println ("Made k synthetic " + sqm.getMetric() + " from " + prefix + " and " + suffix);

				SyntheticMetric generatedMetric = new SyntheticMetric() {
					@Override public String getSyntheicName() { return sqm.getMetric();}
					@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add (prefix);  list.add(suffix);  return list;}
					@Override public double calculate(Map<String,Double> metrics) {
						double denominator = 0;
						Double denominatorInput = metrics.get(suffix);
						if (denominatorInput != null) denominator = denominatorInput;
						double numerator = 0;
						Double numeratorInput = metrics.get(prefix);
						if (numeratorInput != null) numerator = numeratorInput;
						if ((denominator == 0) || (numerator == 0)) return 0.0;
						return (1000.0 * numerator) / denominator;
					}
				};
				syntheticMetrics.put(sqm.getMetric(), generatedMetric);
			}

			offset = sqm.getMetric().indexOf("__PER_H_");
			if (offset != -1) {
				String prefix = sqm.getMetric().substring(0, offset);
				String suffix = sqm.getMetric().substring(offset + 8);

//				System.out.println ("Made h synthetic " + sqm.getMetric() + " from " + prefix + " and " + suffix);

				SyntheticMetric generatedMetric = new SyntheticMetric() {
					@Override public String getSyntheicName() { return sqm.getMetric();}
					@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add (prefix);  list.add(suffix);  return list;}
					@Override public double calculate(Map<String,Double> metrics) {
						double denominator = 0;
						Double denominatorInput = metrics.get(suffix);
						if (denominatorInput != null) denominator = denominatorInput;
						double numerator = 0;
						Double numeratorInput = metrics.get(prefix);
						if (numeratorInput != null) numerator = numeratorInput;
						if ((denominator == 0) || (numerator == 0)) return 0.0;
						return (100.0 * numerator) / denominator;
					}
				};
				syntheticMetrics.put(sqm.getMetric(), generatedMetric);
			}
		}

		
		SyntheticMetric ViolationsPerKLines = new SyntheticMetric() {
			@Override public String getSyntheicName() { return "ViolationsPerKLines";}
			@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add ("violations");  list.add("ncloc");  return list;}
			@Override public double calculate(Map<String,Double> metrics) {
				double lines = 0;
				Double lineInput = metrics.get("ncloc");
				if (lineInput != null) lines = lineInput;
				double violations = 0;
				Double violationsInput = metrics.get("violations");
				if (violationsInput != null) violations = violationsInput;
				if ((lines == 0) || (violations == 0)) return 0.0;
				return (1000.0 * violations) / lines;
			}
		};
		syntheticMetrics.put(ViolationsPerKLines.getSyntheicName(), ViolationsPerKLines);

		
		SyntheticMetric CognitiveComplexityPerKLines = new SyntheticMetric() {
			@Override public String getSyntheicName() { return "CognitiveComplexityPerKLines";}
			@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add ("cognitive_complexity");  list.add("ncloc");  return list;}
			@Override public double calculate(Map<String,Double> metrics) {
				double lines = 0;
				Double lineInput = metrics.get("ncloc");
				if (lineInput != null) lines = lineInput;
				double numerator = 0;
				Double numeratorInput = metrics.get("cognitive_complexity");
				if (numeratorInput != null) numerator = numeratorInput;
				if ((lines == 0) || (numerator == 0)) return 0.0;
				return (1000.0 * numerator) / lines;
			}
		};
		syntheticMetrics.put(CognitiveComplexityPerKLines.getSyntheicName(), CognitiveComplexityPerKLines);


		return syntheticMetrics;
	}

	public static Date getUTCDate (final Date date) {
		return new Date (Date.UTC (
			date.getYear(),
			date.getMonth(),
			date.getDate() + 1,
			0,0,0
		));
	}

}

