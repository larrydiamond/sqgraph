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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.Styler.LegendPosition;

import com.google.common.collect.HashBasedTable;

public class GraphOutput {
    
    private GraphOutput () {}

    public static void outputGraphs (final Config config, final Map<String, AssembledSearchHistory> rawMetrics, 
                              final HashBasedTable<String,String,Double> dashboardData, final Map<String, String> titleLookup, 
                              final Map<String, SyntheticMetric> syntheticMetrics) {
        for (SQMetrics sqm : config.getMetrics()) {
            try {
                XYChart chart = null;
                if (rawMetrics.size() > 7) {
                    chart = new XYChartBuilder()
                    .width(825)
                    .height(600)
                    .title(sqm.getTitle())
                    .xAxisTitle("X")
                    .yAxisTitle("Y")
                    .build();
    
                    chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
                    chart.getStyler().setLegendLayout(Styler.LegendLayout.Vertical);
                } else {
                    chart = new XYChartBuilder()
                    .width(825)
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
                chart.getStyler().setYAxisDecimalPattern(SqgraphApplication.standardDecimalFormat);

                for (Map.Entry<String, AssembledSearchHistory> entry : rawMetrics.entrySet()) {
                    addSeriesForMetric (sqm.getMetric(), entry.getValue(), chart, titleLookup.get (entry.getKey()), syntheticMetrics, dashboardData, sqm.getTitle());
                }

                if (sqm.getFilename() == null)
                    sqm.setFilename(sqm.getTitle());
                
                if (!sqm.getFilename().endsWith(".png"))
                    sqm.setFilename(sqm.getFilename() + ".png");

                BitmapEncoder.saveBitmap(chart, sqm.getFilename(), BitmapFormat.PNG);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	public static Double addSeriesForNativeMetric (final String metricName, final AssembledSearchHistory history, List<Date> dates, List<Double> doubles) {
		Double lastDataPoint = 0.0;
		for (Measures m : history.getMeasures()) {
			if (m.getMetric().equals(metricName)) {
				for (History h : m.getHistory()) {
					dates.add (SqgraphApplication.getUTCDate(h.getDate()));
					doubles.add (h.getValue());
					lastDataPoint = h.getValue();
				}
			}
		}
		return lastDataPoint;
	}

	public static Double addSeriesForSyntheticMetric (final SyntheticMetric sm, final AssembledSearchHistory history, List<Date> dates, List<Double> doubles) {
		Double lastDataPoint = 0.0;

		// find the first real measure needed by the synthetic metric
		// for each of the dates in its history, look for the other metrics on the same dates 
		// populate the String, Double map and invoke the calculate method for each and add that double
		List<String> realMetrics = sm.getRealMetrics();

		boolean notFound = true;
		for (int loop = 0; ((loop < history.getMeasures().size()) && (notFound)); loop++) {
			Measures m = history.getMeasures().get(loop);
			if (realMetrics.contains(m.getMetric())) {
				notFound = false;
				for (History h : m.getHistory()) {
					Date dataPoint = h.getDate();
					dates.add (SqgraphApplication.getUTCDate(dataPoint));

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

	public static void addSeriesForMetric (final String metricName, final AssembledSearchHistory history, final XYChart chart, final String application, 
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
}
