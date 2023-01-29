// Copyright Larry Diamond 2023 All Rights Reserved
package com.ldiamond.sqgraph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.Styler.LegendPosition;
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
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

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

		SimpleDateFormat sdfsq = new SimpleDateFormat("yyyy-MM-dd");
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

		Map<String,SyntheticMetric> syntheticMetrics = populateSynthetics();
//		System.out.println (config.toString());

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

		/*
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
		*/

//		try {
//			final String uri = config.getUrl() + "/api/metrics/types";
//			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(headers), String.class);
//			String result = response.getBody();
//			System.out.println ("metric types : " + result);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}

		Map<String, String> titleLookup = new HashMap<>();

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
//				System.out.println (key + " : " + result);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}


		Document document = new Document(new Rectangle(900, 700));
        try {
            PdfWriter.getInstance(document, new FileOutputStream("Images.pdf"));
            document.open();
			document.addTitle ("Created by the Code Quality Graphing Tool");
			Paragraph paragraph = new Paragraph("Created by the Code Quality Graphing Tool");
			paragraph.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph);

			for (SQMetrics sqm : config.getMetrics()) {
				try {
					XYChart chart = null;
					if (rawMetrics.size() > 7) {
						chart = new XYChartBuilder()
						.width(800)
						.height(600)
						.title(sqm.getTitle())
						.xAxisTitle("X")
						.yAxisTitle("Y")
						.build();
		
						chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
						chart.getStyler().setLegendLayout(Styler.LegendLayout.Vertical);
					} else {
						chart = new XYChartBuilder()
						.width(800)
						.height(600)
						.title(sqm.getTitle())
						.xAxisTitle("X")
						.yAxisTitle("Y")
						.build();
						
						chart.getStyler().setLegendPosition(LegendPosition.OutsideS);
						chart.getStyler().setLegendLayout(Styler.LegendLayout.Horizontal);
					}

					chart.getStyler().setAxisTitlesVisible(false);
					chart.getStyler().setDatePattern("dd MMM yyyy");
					chart.getStyler().setYAxisDecimalPattern("###,###,###.###");

					for (Map.Entry<String, SearchHistory> entry : rawMetrics.entrySet()) {
						addSeriesForMetric (sqm.getMetric(), entry.getValue(), chart, titleLookup.get (entry.getKey()), syntheticMetrics);
					}
				
					BitmapEncoder.saveBitmap(chart, sqm.getFilename(), BitmapFormat.PNG);
	
					Image png = Image.getInstance(sqm.getFilename() + ".png");
					document.add(png);

				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		} catch(DocumentException de) {
			System.err.println(de.getMessage());
		}
		catch(IOException ioe) {
			System.err.println(ioe.getMessage());
		}
	
		document.close();

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
		String output = "";
		boolean comma = false;
		List<String> alreadyAdded = new ArrayList<>();
		for (String metric : metrics) {
			if (!alreadyAdded.contains(metric)) {
				alreadyAdded.add(metric);
				if (!comma) {
					comma = true;
				} else {
					output = output + ",";
				}
				output = output + metric;
			}
		}
		return output;
	}

	public static Map<String,SyntheticMetric> populateSynthetics () {
		Map<String,SyntheticMetric> syntheticMetrics = new HashMap<>();
		
		SyntheticMetric ViolationsPerKLines = new SyntheticMetric() {
			@Override public String getSyntheicName() { return "ViolationsPerKLines";}
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
		syntheticMetrics.put(ViolationsPerKLines.getSyntheicName(), ViolationsPerKLines);

		
		SyntheticMetric CognitiveComplexityPerKLines = new SyntheticMetric() {
			@Override public String getSyntheicName() { return "CognitiveComplexityPerKLines";}
			@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add ("cognitive_complexity");  list.add("lines");  return list;}
			@Override public double calculate(Map<String,Double> metrics) {
				double lines = 0;
				Double lineInput = metrics.get("lines");
				if (lineInput != null) lines = lineInput;
				double numerator = 0;
				Double numeratorInput = metrics.get("cognitive_complexity");
				if (numeratorInput != null) numerator = numeratorInput;
				if ((lines == 0) || (numerator == 0)) return 0.0;
				return numerator / lines;
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

	public static void addSeriesForMetric (final String metricName, final SearchHistory history, final XYChart chart, final String application, final Map<String,SyntheticMetric> syntheticMetrics) {
		List<Date> dates = new ArrayList<>();
		List<Double> doubles = new ArrayList<>();

		SyntheticMetric sm = syntheticMetrics.get(metricName);
		if (sm == null) {
			for (Measures m : history.getMeasures()) {
				if (m.getMetric().equals(metricName)) {
					for (History h : m.getHistory()) {
						dates.add (getUTCDate(h.getDate()));
						doubles.add (h.getValue());
					}
				}
			}
		} else {
			// find the first real measure needed by the synthetic metric
			// for each of the dates in its history, look for the other metrics on the same dates 
			// populate the String, Double map and invoke the calculate method for each and add that double
			List<String> realMetrics = sm.getRealMetrics();

			boolean notFound = true;
			for (int loop = 0; ((loop < history.getMeasures().length) && (notFound)); loop++) {
				Measures m = history.getMeasures() [loop];
				if (realMetrics.contains(m.getMetric())) {
					notFound = false;
					for (History h : m.getHistory()) {
						Date dataPoint = h.getDate();
						dates.add (getUTCDate(dataPoint));

						Map<String,Double> values = new HashMap<>();
						values.put(m.getMetric(), h.getValue());
						for (Measures measure : history.getMeasures()) {
							if (realMetrics.contains(measure.getMetric())) {
								History[] histArray = measure.getHistory();
								for (History hist : histArray) {
									if (hist.getDate().equals(dataPoint)) {
										values.put (measure.getMetric(), hist.getValue());
									}
								}
							}
						}

						double calculatedValue = sm.calculate(values);
						doubles.add (calculatedValue);
					}
				}
			}
		}

		if (!dates.isEmpty() && (!doubles.isEmpty()))
			chart.addSeries(application, dates, doubles);
	}


}
