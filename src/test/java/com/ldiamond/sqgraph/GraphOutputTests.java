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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GraphOutputTests {

	@Test
	void testAddSeriesForNativeMetric() {
        List<Date> dates = new ArrayList<>();
        List<Double> doubles = new ArrayList<>();
        AssembledSearchHistory history = new AssembledSearchHistory();
        List<Measures> measureList = new ArrayList<>();
        history.setMeasures(measureList);
        Measures m = new Measures();
        measureList.add(m);
        m.setMetric("unittest");
        History[] historyArray = new History[2];
        m.setHistory(historyArray);
        History h0 = new History();
        historyArray[0] = h0;
        h0.setDate(new Date("Sat, 12 Aug 1995 13:30:00 GMT"));
        h0.setValue(100.0);
        History h1 = new History();
        historyArray[1] = h1;
        h1.setDate(new Date("Sat, 19 Aug 1995 13:30:00 GMT"));
        h1.setValue(200.0);
        Double lastPoint = GraphOutput.addSeriesForNativeMetric("unittest", history, dates, doubles);
        assertEquals (200.0, lastPoint);
		assertEquals (2, dates.size());
		assertEquals ("Sat Aug 12 20:00:00 EDT 1995", dates.getFirst().toString());
		assertEquals ("Sat Aug 19 20:00:00 EDT 1995", dates.get(1).toString());
		assertEquals (2, doubles.size());
        assertEquals (100.0, doubles.getFirst());
        assertEquals (200.0, doubles.get(1));
	}



	static SyntheticMetric unitTestSyntheticMetric = new SyntheticMetric() {
		@Override public String getSyntheticName() { return "unitTestSyntheticMetric";}
		@Override public List<String> getRealMetrics() { List<String> list = new ArrayList<>();  list.add ("unittest");  return list;}
		@Override public double calculate(Map<String,Double> metrics) {
			double lines = 0;
			Double lineInput = metrics.get("unittest");
			if (lineInput != null) lines = lineInput;
			return lines * 2.0;
		}
	};

	@Test
	void testAddSeriesForSyntheticMetric() {
        List<Date> dates = new ArrayList<>();
        List<Double> doubles = new ArrayList<>();
        AssembledSearchHistory history = new AssembledSearchHistory();
        List<Measures> measureList = new ArrayList<>();
        history.setMeasures(measureList);
        Measures m = new Measures();
        measureList.add(m);
        m.setMetric("unittest");
        History[] historyArray = new History[2];
        m.setHistory(historyArray);
        History h0 = new History();
        historyArray[0] = h0;
        h0.setDate(new Date("Sat, 12 Aug 1995 13:30:00 GMT"));
        h0.setValue(50.0);
        History h1 = new History();
        historyArray[1] = h1;
        h1.setDate(new Date("Sat, 19 Aug 1995 13:30:00 GMT"));
        h1.setValue(200.0);

        Double lastPoint = GraphOutput.addSeriesForSyntheticMetric(unitTestSyntheticMetric, history, dates, doubles);
        assertEquals(400.0, lastPoint);
		assertEquals (2, dates.size());
		assertEquals ("Sat Aug 12 20:00:00 EDT 1995", dates.getFirst().toString());
		assertEquals ("Sat Aug 19 20:00:00 EDT 1995", dates.get(1).toString());
		assertEquals (2, doubles.size());
        assertEquals (100.0, doubles.getFirst());
        assertEquals (400.0, doubles.get(1));

    }
}
