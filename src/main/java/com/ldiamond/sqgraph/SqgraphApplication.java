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

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.lowagie.text.Document;

@SpringBootApplication
public class SqgraphApplication {
	static String login = null;
	static String filename = null;

	public static void main(String[] args) {
		login = getSonarQubeToken();
		if (login == null) {
			System.out.println ("Please create a user token in your SonarQube server and set that token in your environment as SONAR_TOKEN");
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

	static String getSonarQubeToken() {
		String login = System.getenv("SONARLOGIN");
		if (login != null) 
			return login;

		login = System.getenv("SONAR_TOKEN");
		if (login != null) 
			return login;

		login = System.getenv("SONAR_LOGIN");
		if (login != null) 
			return login;

		login = System.getenv("SONARTOKEN");
		if (login != null) 
			return login;

		return null;
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
		String base64 = "Basic " + Base64.getEncoder().encodeToString ((login + ":").getBytes());
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

		// Copy the config applications to the expanded applications and perform any searches
		config.setExpandedApplications(new ArrayList<>());
		for (Application app : config.getApplications()) {
			if (app.getKey() != null) {
				config.getExpandedApplications().add(app);
			} else {
				if (app.getQuery() != null) {
					final String uri = config.getUrl() + "/api/projects/search?qualifiers=TRK&q=" + app.getQuery();
					ResponseEntity<ApiProjectsSearchResults> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), ApiProjectsSearchResults.class);
					ApiProjectsSearchResults result = response.getBody();
					if ((result != null) && (result.getComponents() != null)) {
						for (ApiProjectsSearchResultsComponents c : result.getComponents()) {
							config.getExpandedApplications().add(c.getApplication());
						}
					}
				}
			}
		}

		Map<String, String> titleLookup = new HashMap<>();
		final DateTimeFormatter sdfsq = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		Map<String, AssembledSearchHistory> rawMetrics = new HashMap<>();
		for (Application app : config.getExpandedApplications()) {
			String key = app.getKey();
			titleLookup.put(key, app.getTitle());
			try {
				List<String> metricsToQuery = getMetricsListNeeded(config,syntheticMetrics);
				String metrics = getCommaSeparatedListOfMetrics (metricsToQuery);
				
				Date startDate = new Date();
				startDate = DateUtils.addDays (startDate, (-1 * config.getMaxReportHistory()));
				LocalDate localDate = startDate.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
				final String sdfsqString = sdfsq.format (localDate);

				AssembledSearchHistory history = getHistory (config, sdfsqString, key, metrics, headers, restTemplate);
				rawMetrics.put (key, history);

				Thread.sleep(1); // SonarCloud implemented rate limiting, https://docs.github.com/en/rest/rate-limit?apiVersion=2022-11-28, sorry for contributing to the problem.   I guess we all got popular :)

			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		HashBasedTable<String,String,Double> dashboardData = HashBasedTable.create(config.getMetrics().length, 100);

		GraphOutput.outputGraphs(config, rawMetrics, dashboardData, titleLookup, syntheticMetrics);

		if (config.getPdf() != null) {
			Document document = PDFOutput.createPDF (config);
			PDFOutput.addTextDashboard (document, dashboardData, config);
			PDFOutput.addGraphs(document, config);
			PDFOutput.closePDF(document);
		}

		System.out.println ("Successful completion.");
		return new CommandLineRunner() {
			@Override
			public void run(String... args) throws Exception {
				// nothing to do
			}
		};
	}

	public static AssembledSearchHistory getHistory (final Config config, final String sdfsqString, final String key, final String metrics, 
	final HttpHeaders headers, RestTemplate restTemplate) {
		AssembledSearchHistory assembledSearchHistory = new AssembledSearchHistory();
		int page = 1;
		boolean notYetLastPage = true;
		do {
			final String uri = config.getUrl() + "/api/measures/search_history?from="+sdfsqString+"&p="+page+"&ps=999&component=" + key + "&metrics=" + metrics;
			System.out.println (uri);
			ResponseEntity<SearchHistory> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), SearchHistory.class);
			SearchHistory result = response.getBody();
			if (result != null) {
				if ((result.getPaging() != null) && (result.getPaging().total <= page)) {
					notYetLastPage = false;
					try {
						Thread.sleep(1); // SonarCloud implemented rate limiting, https://docs.github.com/en/rest/rate-limit?apiVersion=2022-11-28, sorry for contributing to the problem.   I guess we all got popular :)
					} catch (InterruptedException ie) { }

				}
				if (result.getMeasures() != null) {
					List<Measures> measures = assembledSearchHistory.getMeasures();
					if (measures == null) {
						measures = new ArrayList<>();
						assembledSearchHistory.setMeasures(measures);
					}
					measures.addAll(Arrays.asList(result.getMeasures()));
				}
			}
			page++;
		} while (notYetLastPage);
		return assembledSearchHistory;
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

	static final SyntheticMetric ViolationsPerKLines = getMetric ("ViolationsPerKLines", "violations", "ncloc", 1000.0);
	
	static final SyntheticMetric CognitiveComplexityPerKLines = getMetric ("CognitiveComplexityPerKLines", "cognitive_complexity", "ncloc", 1000.0);

	static final SyntheticMetric bugsPlusSecurity = new SyntheticMetric() {
		@Override public String getSyntheticName() { return "BugsPlusSecurity";}
		@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add ("bugs");  list.add ("vulnerabilities");  list.add("security_hotspots");  return list;}
		@Override public double calculate(Map<String,Double> metrics) {
			double bugs = 0;
			Double bugsInput = metrics.get("bugs");
			if (bugsInput != null) bugs = bugsInput;
			
			double vulnerabilities = 0;
			Double vulnInput = metrics.get("vulnerabilities");
			if (vulnInput != null) vulnerabilities = vulnInput;
			
			double sech = 0;
			Double sechInput = metrics.get("security_hotspots");
			if (sechInput != null) sech = sechInput;
			
			return bugs + vulnerabilities + sech;
		}
	};

	public static Map<String,SyntheticMetric> populateSynthetics (final Config config) {
		Map<String,SyntheticMetric> syntheticMetrics = new HashMap<>();

		for (SQMetrics sqm : config.getMetrics()) {
			int offset = sqm.getMetric().indexOf("__PER__");
			if (offset != -1) {
				String prefix = sqm.getMetric().substring(0, offset);
				String suffix = sqm.getMetric().substring(offset + 7);
				syntheticMetrics.put(sqm.getMetric(), getMetric (sqm.getMetric(), prefix, suffix, 1.0));
			}

			offset = sqm.getMetric().indexOf("__PER_K_");
			if (offset != -1) {
				String prefix = sqm.getMetric().substring(0, offset);
				String suffix = sqm.getMetric().substring(offset + 8);
				syntheticMetrics.put(sqm.getMetric(), getMetric (sqm.getMetric(), prefix, suffix, 1000.0));
			}

			offset = sqm.getMetric().indexOf("__PER_H_");
			if (offset != -1) {
				String prefix = sqm.getMetric().substring(0, offset);
				String suffix = sqm.getMetric().substring(offset + 8);
				syntheticMetrics.put(sqm.getMetric(), getMetric (sqm.getMetric(), prefix, suffix, 100.0));
			}
		}

		syntheticMetrics.put(ViolationsPerKLines.getSyntheticName(), ViolationsPerKLines);
		syntheticMetrics.put(CognitiveComplexityPerKLines.getSyntheticName(), CognitiveComplexityPerKLines);
		syntheticMetrics.put(bugsPlusSecurity.getSyntheticName(), bugsPlusSecurity);

		return syntheticMetrics;
	}

	private static SyntheticMetric getMetric (final String metricName, final String numeratorMetric, final String denominatorMetric, final double multiplier) {
		return new SyntheticMetric() {
			@Override public String getSyntheticName() { return metricName;}
			@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add (numeratorMetric);  list.add(denominatorMetric);  return list;}
			@Override public double calculate(Map<String,Double> metrics) {
				double denominator = 0;
				Double denominatorInput = metrics.get(denominatorMetric);
				if (denominatorInput != null) denominator = denominatorInput;
				double numerator = 0;
				Double numeratorInput = metrics.get(numeratorMetric);
				if (numeratorInput != null) numerator = numeratorInput;
				if ((denominator == 0) || (numerator == 0)) return 0.0;
				return (multiplier * numerator) / denominator;
			}
		};
	}
}

