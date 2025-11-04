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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knowm.xchart.XYChart;

import com.google.common.collect.HashBasedTable;

class GraphOutputTests {

	@Test
	void testAddSeriesForNativeMetric() throws ParseException {
		final List<Date> dates = new ArrayList<>();
		final List<Double> doubles = new ArrayList<>();
		final AssembledSearchHistory history = new AssembledSearchHistory();
		final List<Measures> measureList = new ArrayList<>();
        history.setMeasures(measureList);
		final Measures m = new Measures();
        measureList.add(m);
        m.setMetric("unittest");
		final History[] historyArray = new History[2];
        m.setHistory(historyArray);
		final History h0 = new History();
        historyArray[0] = h0;
        h0.setDate(parseRfc822("Sat, 12 Aug 1995 13:30:00 GMT"));
        h0.setValue(100.0);
		final History h1 = new History();
        historyArray[1] = h1;
        h1.setDate(parseRfc822("Sat, 19 Aug 1995 13:30:00 GMT"));
        h1.setValue(200.0);
		final Double lastPoint = GraphOutput.addSeriesForNativeMetric("unittest", history, dates, doubles);
        assertEquals (200.0, lastPoint);
		assertEquals (2, dates.size());
//		assertEquals ("Sat Aug 12 20:00:00 EDT 1995", dates.getFirst().toString());
//		assertEquals ("Sat Aug 19 20:00:00 EDT 1995", dates.get(1).toString());
		assertEquals (2, doubles.size());
        assertEquals (100.0, doubles.getFirst());
        assertEquals (200.0, doubles.get(1));
	}

	static final SyntheticMetric unitTestSyntheticMetric = new SyntheticMetric() {
		@Override public String getSyntheticName() { return "unitTestSyntheticMetric";}
		@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add ("unittest");  return list;}
		@Override public double calculate(Map<String,Double> metrics) {
			double lines = 0;
			Double lineInput = metrics.get("unittest");
			if (lineInput != null) lines = lineInput;
			return lines * 2.0;
		}
    };
    
    private static Date parseRfc822(String s) throws ParseException {
        return new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH).parse(s);
    }
    
    @Test
    void testAddSeriesForSyntheticMetric() throws ParseException {
		final List<Date> dates = new ArrayList<>();
		final List<Double> doubles = new ArrayList<>();

		final AssembledSearchHistory history = new AssembledSearchHistory();
		final List<Measures> measureList = new ArrayList<>();
        history.setMeasures(measureList);
		final Measures m = new Measures();
        measureList.add(m);

        m.setMetric("unittest");
		final History[] historyArray = new History[2];
        m.setHistory(historyArray);
		final History h0 = new History();
        historyArray[0] = h0;
        h0.setDate(parseRfc822("Sat, 12 Aug 1995 13:30:00 GMT"));
        h0.setValue(50.0);
		final History h1 = new History();
        historyArray[1] = h1;
        h1.setDate(parseRfc822("Sat, 19 Aug 1995 13:30:00 GMT"));
        h1.setValue(200.0);

		final Double lastPoint = GraphOutput.addSeriesForSyntheticMetric(unitTestSyntheticMetric, history, dates, doubles);
        assertEquals(400.0, lastPoint);
		assertEquals (2, dates.size());
//		assertEquals ("Sat Aug 12 20:00:00 EDT 1995", dates.getFirst().toString());
//		assertEquals ("Sat Aug 19 20:00:00 EDT 1995", dates.get(1).toString());
		assertEquals (2, doubles.size());
        assertEquals (100.0, doubles.getFirst());
        assertEquals (400.0, doubles.get(1));
    }

    @Test
    void testAddSeriesForNativeMetricNoData() {
		final List<Date> dates = new ArrayList<>();
		final List<Double> doubles = new ArrayList<>();
		final AssembledSearchHistory history = new AssembledSearchHistory();
		final List<Measures> measureList = new ArrayList<>();
        history.setMeasures(measureList);
		final Measures linesOfCode = new Measures();
        measureList.add(linesOfCode);
        linesOfCode.setMetric("linesOfCode");
		final History[] historyArray = new History[0];
        linesOfCode.setHistory(historyArray);
		final Measures violations = new Measures();
        measureList.add(violations);
        violations.setMetric("violations");
        violations.setHistory(historyArray);
		final Double lastPoint = GraphOutput.addSeriesForNativeMetric("linesOfCode", history, dates, doubles);
        assertEquals (0.0, lastPoint, 0.1);
    }

    @Test
    void testAddSeriesForNativeMetricWithData() throws ParseException {
		final List<Date> dates = new ArrayList<>();
		final List<Double> doubles = new ArrayList<>();
		final AssembledSearchHistory history = new AssembledSearchHistory();
		final List<Measures> measureList = new ArrayList<>();
        history.setMeasures(measureList);
		final Measures linesOfCode = new Measures();
        measureList.add(linesOfCode);
        linesOfCode.setMetric("linesOfCode");
		final History[] historyArray = new History[2];
        linesOfCode.setHistory(historyArray);
        historyArray[0] = new History();
        historyArray[0].setDate(parseRfc822("Sat, 12 Aug 1995 13:30:00 GMT"));
        historyArray[0].setValue(500.0);
        historyArray[1] = new History();
        historyArray[1].setDate(parseRfc822("Sat, 19 Aug 1995 13:30:00 GMT"));
        historyArray[1].setValue(1000.0);
		final Double lastPoint = GraphOutput.addSeriesForNativeMetric("linesOfCode", history, dates, doubles);
        assertEquals (1000.0, lastPoint, 0.1);
    }


    @Test
    void testAddSeriesForMetricNative() throws ParseException {
		final XYChart chart = mock(XYChart.class);
		final AssembledSearchHistory history = new AssembledSearchHistory();
		final List<Measures> measureList = new ArrayList<>();
        history.setMeasures(measureList);
		final Measures linesOfCode = new Measures();
        measureList.add(linesOfCode);
        linesOfCode.setMetric("linesOfCode");
		final History[] historyArray = new History[2];
        linesOfCode.setHistory(historyArray);
        historyArray[0] = new History();
        historyArray[0].setDate(parseRfc822("Sat, 12 Aug 1995 13:30:00 GMT"));
        historyArray[0].setValue(500.0);
        historyArray[1] = new History();
        historyArray[1].setDate(parseRfc822("Sat, 19 Aug 1995 13:30:00 GMT"));
        historyArray[1].setValue(1000.0);
        GraphOutput.addSeriesForMetric("linesOfCode", history, chart, "MyApp", Map.of(), HashBasedTable.create(20, 100), "Lines of Code");
        verify(chart, times(1)).addSeries(any(), any(List.class), any(List.class));
    }


}
