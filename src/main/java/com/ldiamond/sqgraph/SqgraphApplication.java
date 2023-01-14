package com.ldiamond.sqgraph;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.apache.commons.lang3.time.DateUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class SqgraphApplication {
	static String login = null;
	static String filename = null;

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

		System.out.println (config.toString());

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		String base64 = "Basic " + new String (Base64.getEncoder().encodeToString (new String (login + ":").getBytes()));
		headers.set ("Authorization", base64);

		try {
			final String uri = config.getUrl() + "/api/authentication/validate";
			ResponseEntity<ValidationResult> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), ValidationResult.class);
			ValidationResult result = response.getBody();
			if (!result.isValid()) {
				System.out.println ("SonarQube login token was not valid");
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		try {
			final String uri = config.getUrl() + "/api/metrics/search";
			ResponseEntity<MetricsResults> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), MetricsResults.class);
			MetricsResults result = response.getBody();
			ConcurrentSkipListMap<String, Metric> ms = new ConcurrentSkipListMap<>();
			for (Metric m : result.getMetrics()) {
				ms.put (m.getKey(), m);
			}
			for (Map.Entry<String, Metric> me : ms.entrySet()) {
				System.out.println (me.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		try {
			final String uri = config.getUrl() + "/api/metrics/types";
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), String.class);
			String result = response.getBody();
			System.out.println ("metric types : " + result);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		HashMap<String, String> titleLookup = new HashMap<>();

		HashMap<String, SearchHistory> rawMetrics = new HashMap<>();
		for (Application app : config.getApplications()) {
			String key = app.getKey();
			titleLookup.put(key, app.getTitle());
			try {
				String metrics = "";
				boolean comma = false;
				for (SQMetrics sqm : config.getMetrics()) {
					if (!comma) {
						comma = true;
					} else {
						metrics = metrics + ",";
					}
					metrics = metrics + sqm.getMetric();
				}

				final String uri = config.getUrl() + "/api/measures/search_history?component=" + key + "&metrics=" + metrics;
				ResponseEntity<SearchHistory> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), SearchHistory.class);
				SearchHistory result = response.getBody();
				rawMetrics.put (key, result);
				System.out.println (key + " : " + result);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		for (SQMetrics sqm : config.getMetrics()) {
			try {
				XYChart chart = new XYChartBuilder()
				.width(800)
				.height(600)
				.title(sqm.getTitle())
				.xAxisTitle("X")
				.yAxisTitle("Y")
				.build();
	
				chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
				chart.getStyler().setAxisTitlesVisible(false);
				chart.getStyler().setLegendPosition(LegendPosition.OutsideS);
				chart.getStyler().setLegendLayout(Styler.LegendLayout.Horizontal);
				chart.getStyler().setDatePattern("dd MMM yyyy");

				for (Map.Entry<String, SearchHistory> entry : rawMetrics.entrySet()) {
					List<Date> dates = new ArrayList<>();
					List<Double> doubles = new ArrayList<>();
	
						for (Measures m : entry.getValue().getMeasures()) {
						if (m.getMetric().endsWith(sqm.getMetric())) {
							for (History h : m.getHistory()) {

								Date utcDate = new Date (Date.UTC (
									
									h.getDate().getYear(),
									h.getDate().getMonth(),
									h.getDate().getDate() + 1,
									0,0,0
								));

								dates.add (utcDate);
								doubles.add (h.getValue());
							}
						}
					}
					chart.addSeries(titleLookup.get (entry.getKey()), dates, doubles);
				}
				

				BitmapEncoder.saveBitmap(chart, sqm.getFilename(), BitmapFormat.PNG);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		System.out.println ("no failures yet");
		return null;
	}
}
