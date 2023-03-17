// Copyright Larry Diamond 2023 All Rights Reserved
package com.ldiamond.sqgraph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;

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

import com.google.common.collect.HashBasedTable;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.FontMetrics;

@SpringBootApplication
public class SqgraphApplication {
	static String login = null;
	static String filename = null;
	static String standardDecimalFormat = "###,###,###.###";
	static DecimalFormat standardDecimalFormatter = new DecimalFormat (standardDecimalFormat);

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

		Document document = null;
		if (config.getPdf() != null)
			document = new Document(new Rectangle(900, 700));
        try {
			if (document != null) {
				PdfWriter.getInstance(document, new FileOutputStream(config.getPdf()));
				document.open();
				document.addTitle ("Code Quality Graphs");
				Paragraph paragraph = new Paragraph("Created by the Code Quality Graphing Tool");
				paragraph.setAlignment(Element.ALIGN_CENTER);
				document.add(paragraph);
			}

			HashBasedTable<String,String,Double> dashboardData = HashBasedTable.create(10, 10);

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
					chart.getStyler().setYAxisDecimalPattern(standardDecimalFormat);

					for (Map.Entry<String, SearchHistory> entry : rawMetrics.entrySet()) {
						addSeriesForMetric (sqm.getMetric(), entry.getValue(), chart, titleLookup.get (entry.getKey()), syntheticMetrics, dashboardData, sqm.getTitle());
					}

					if (sqm.getFilename() != null) {
						String pngfilename = sqm.getFilename();
						if (!pngfilename.endsWith(".png")) {
							pngfilename += ".png";
						}

						BitmapEncoder.saveBitmap(chart, pngfilename, BitmapFormat.PNG);
	
						if (document != null) {
							Image png = Image.getInstance(pngfilename);
							document.add(png);
						}
					}
				

				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			String [] dashboardColumns = new String [1 + dashboardData.rowKeySet().size()];
			dashboardColumns [0] = "";
			int dcOffset = 1;
			for (String dcCol : dashboardData.rowKeySet()) {
				dashboardColumns [dcOffset++] = dcCol;
			}

			String [] [] dashboardFormattedData = new String [config.getApplications().length] [];

			int rowLoop = 0;
			for (Application app : config.getApplications()) {
				Map<String,Double> rowMap = dashboardData.column(app.getTitle());
				String [] dRow = new String [1 + dashboardData.rowKeySet().size()];
				dRow [0] = app.getTitle();
				int colLoop = 1;
				for (String dcCol : dashboardData.rowKeySet()) {
					dRow [colLoop] = standardDecimalFormatter.format (rowMap.get(dcCol));
					colLoop++;
				}
				dashboardFormattedData[rowLoop] = dRow;
				rowLoop++;
			}

			JTable jt = new JTable(dashboardFormattedData, dashboardColumns);
			sizeColumnsToFit(jt, 15);
			jt.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			jt.setGridColor(new Color(115,52,158));
			jt.setRowMargin(5);
			jt.setShowGrid(true);
			jt.doLayout();

			JScrollPane scroll = new JScrollPane(jt);
			JPanel p = new JPanel(new BorderLayout());
			p.add(scroll,BorderLayout.CENTER);

			p.addNotify();
			p.setSize(getTableWidth(jt) + 5, getTableHeight(jt));
			p.validate();

			BufferedImage bi = new BufferedImage(
				(int)p.getSize().getWidth(),
				(int)p.getSize().getHeight(),
				BufferedImage.TYPE_INT_RGB
				);
	
			Graphics g = bi.createGraphics();
			p.paint(g);

			int scaledWidth = Math.min(800, (int)p.getSize().getWidth());
			int scaledHeight = (((int)p.getSize().getHeight()) * scaledWidth) / ((int)p.getSize().getWidth());

			java.awt.Image scaled = bi.getScaledInstance(scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH);
			BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
			outputImage.getGraphics().drawImage(scaled, 0, 0, null);

			String dashboardfile = config.getDashboard();
			if (dashboardfile == null) 
				dashboardfile = "dashboard.png";
			ImageIO.write(bi,"png",new File(dashboardfile));
			g.dispose();

			if (document != null) {
				Image png = Image.getInstance(outputImage, null);
				document.add(png);
			}
		} catch(DocumentException de) {
			System.err.println(de.getMessage());
		}
		catch(IOException ioe) {
			System.err.println(ioe.getMessage());
		}
	
		if (document != null) 
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

	public static Double addSeriesForNativeMetric (final String metricName, final SearchHistory history, List<Date> dates, List<Double> doubles) {
		Double lastDataPoint = 0.0;
		for (Measures m : history.getMeasures()) {
			if (m.getMetric().equals(metricName)) {
				for (History h : m.getHistory()) {
					dates.add (getUTCDate(h.getDate()));
					doubles.add (h.getValue());
					lastDataPoint = h.getValue();
				}
			}
		}
		return lastDataPoint;
	}

	public static Double addSeriesForSyntheticMetric (final SyntheticMetric sm, final SearchHistory history, List<Date> dates, List<Double> doubles) {
		Double lastDataPoint = 0.0;

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
					lastDataPoint = calculatedValue;
				}
			}
		}
		return lastDataPoint;
	}

	public static void addSeriesForMetric (final String metricName, final SearchHistory history, final XYChart chart, final String application, 
	final Map<String,SyntheticMetric> syntheticMetrics, HashBasedTable<String, String, Double> dashboardData, String metricTitle) {
		List<Date> dates = new ArrayList<>();
		List<Double> doubles = new ArrayList<>();
		Double lastDataPoint = null;
		SyntheticMetric sm = syntheticMetrics.get(metricName);
		if (sm == null) {
			lastDataPoint = addSeriesForNativeMetric (metricName, history, dates, doubles);
		} else {
			lastDataPoint = addSeriesForSyntheticMetric (sm, history, dates, doubles);
		}

		if (!dates.isEmpty() && (!doubles.isEmpty()))
			chart.addSeries(application, dates, doubles);

		dashboardData.put (metricTitle, application, lastDataPoint);
	}



	public static int getTableWidth (final JTable table) {
		int width = 0;

		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

		for(int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
			width += table.getColumnModel().getColumn(columnIndex).getMaxWidth();
			if (columnIndex > 0)
				table.getColumnModel().getColumn(columnIndex).setCellRenderer(rightRenderer);
		}

		return width;
	}

	public static int getTableHeight (final JTable table) {
		int height = table.getTableHeader().getHeight();

		for(int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
			height += table.getRowHeight(rowIndex);
		}

		return Math.max(height, (2 + table.getRowCount()) * table.getRowHeight());
	}

	public static void sizeColumnsToFit(JTable table, int columnMargin) {
        JTableHeader tableHeader = table.getTableHeader();
 
        if(tableHeader == null) {
            // can't auto size a table without a header
            return;
        }
 
        FontMetrics headerFontMetrics = tableHeader.getFontMetrics(tableHeader.getFont());
 
        int[] minWidths = new int[table.getColumnCount()];
        int[] maxWidths = new int[table.getColumnCount()];
 
        for(int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
            int headerWidth = headerFontMetrics.stringWidth(table.getColumnName(columnIndex));
            minWidths[columnIndex] = headerWidth + columnMargin;
            int maxWidth = getMaximalRequiredColumnWidth(table, columnIndex, headerWidth);
            maxWidths[columnIndex] = Math.max(maxWidth, minWidths[columnIndex]) + columnMargin;
        }
 
        adjustMaximumWidths(table, minWidths, maxWidths);
        for(int i = 0; i < minWidths.length; i++) {
            if(minWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setMinWidth(minWidths[i]);
            }
 
            if(maxWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setMinWidth(maxWidths[i]);
                table.getColumnModel().getColumn(i).setMaxWidth(maxWidths[i]);
                table.getColumnModel().getColumn(i).setWidth(maxWidths[i]);
            }
        }
    }
 
    private static void adjustMaximumWidths(JTable table, int[] minWidths, int[] maxWidths) {
        if(table.getWidth() > 0) {
            // to prevent infinite loops in exceptional situations
            int breaker = 0;
 
            // keep stealing one pixel of the maximum width of the highest column until we can fit in the width of the table
            while(sum(maxWidths) > table.getWidth() && breaker < 10000) {
                int highestWidthIndex = findLargestIndex(maxWidths);
                maxWidths[highestWidthIndex] -= 1;
                maxWidths[highestWidthIndex] = Math.max(maxWidths[highestWidthIndex], minWidths[highestWidthIndex]);
                breaker++;
            }
        }
    }
 
    private static int getMaximalRequiredColumnWidth(JTable table, int columnIndex, int headerWidth) {
        int maxWidth = headerWidth;
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        TableCellRenderer cellRenderer = column.getCellRenderer();
        if(cellRenderer == null) {
            cellRenderer = new DefaultTableCellRenderer();
        }
 
        for(int row = 0; row < table.getModel().getRowCount(); row++) {
            Component rendererComponent = cellRenderer.getTableCellRendererComponent(table,
                table.getModel().getValueAt(row, columnIndex),
                false,
                false,
                row,
                columnIndex);
 
            double valueWidth = rendererComponent.getPreferredSize().getWidth();
            maxWidth = (int) Math.max(maxWidth, valueWidth);
        }
 
        return maxWidth;
    }
 
    private static int findLargestIndex(int[] widths) {
        int largestIndex = 0;
        int largestValue = 0;
 
        for(int i = 0; i < widths.length; i++) {
            if(widths[i] > largestValue) {
                largestIndex = i;
                largestValue = widths[i];
            }
        }
 
        return largestIndex;
    }
 
    private static int sum(int[] widths) {
        int sum = 0;
 
        for(int width : widths) {
            sum += width;
        }
 
        return sum;
    }


}
