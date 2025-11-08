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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.HashBasedTable;
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPRow;
import com.lowagie.text.pdf.PdfPTable;

class PDFOutputTests {

    @Test
    void testAddNoGraphsReturnsSameDocument() {
		final Document document = new Document(new Rectangle(1800, (1400 * 10)));
		final Config config = new Config();
        config.setMetrics(new SQMetrics[0]);
		final Document resultDocument = PDFOutput.addGraphs(document, config);
        assertEquals (document, resultDocument);
    }

    @Test
    void testGetWidthOfStringForBlah() {
		final int width = PDFOutput.getWidthOfString("Blah");
        assertEquals (10, width);
    }

    @Test 
    void testSetMax() {
		final List<Integer> widths = new ArrayList<>();
        widths.add(5);
        widths.add(7000);
        PDFOutput.setMax(widths, 0, "Blah", 10);
        PDFOutput.setMax(widths, 1, "Blah", 10);
        assertEquals (20, widths.getFirst());
        assertEquals (7000, widths.get(1));
    }

    @Test
    void testSetBackgroundColorForCellNoColor() {
		final PdfPCell cell = new PdfPCell();
		final SQMetrics metrics = new SQMetrics();
        PDFOutput.setBackgroundColorForCell(cell, metrics, "60.0");
		assertNull(cell.getBackgroundColor());
    }

    @Test
    void testSetBackgroundColorForCellHigherGreen() {
		final PdfPCell cell = new PdfPCell();
		final SQMetrics metrics = new SQMetrics();
        metrics.setGreen("70.0");
        metrics.setYellow("50.0");
        PDFOutput.setBackgroundColorForCell(cell, metrics, "80.0");
        assertEquals (Color.GREEN, cell.getBackgroundColor());
    }

    @Test
    void testSetBackgroundColorForCellHigherYellow() {
		final PdfPCell cell = new PdfPCell();
		final SQMetrics metrics = new SQMetrics();
        metrics.setGreen("70.0");
        metrics.setYellow("50.0");
        PDFOutput.setBackgroundColorForCell(cell, metrics, "60.0");
        assertEquals (Color.YELLOW, cell.getBackgroundColor());
    }

    @Test
    void testSetBackgroundColorForCellHigherRed() {
		final PdfPCell cell = new PdfPCell();
		final SQMetrics metrics = new SQMetrics();
        metrics.setGreen("70.0");
        metrics.setYellow("50.0");
        PDFOutput.setBackgroundColorForCell(cell, metrics, "40.0");
        assertEquals (Color.PINK, cell.getBackgroundColor());
    }
    
    @Test
    void testSetBackgroundColorForCellLowerGreen() {
		final PdfPCell cell = new PdfPCell();
		final SQMetrics metrics = new SQMetrics();
        metrics.setGreen("50.0");
        metrics.setYellow("70.0");
        PDFOutput.setBackgroundColorForCell(cell, metrics, "40.0");
        assertEquals (Color.GREEN, cell.getBackgroundColor());
    }
    
    @Test
    void testSetBackgroundColorForCellLowerYellow() {
		final PdfPCell cell = new PdfPCell();
		final SQMetrics metrics = new SQMetrics();
        metrics.setGreen("50.0");
        metrics.setYellow("70.0");
        PDFOutput.setBackgroundColorForCell(cell, metrics, "60.0");
        assertEquals (Color.YELLOW, cell.getBackgroundColor());
    }

    @Test
    void testSetBackgroundColorForCellLowedRed() {
		final PdfPCell cell = new PdfPCell();
		final SQMetrics metrics = new SQMetrics();
        metrics.setGreen("50.0");
        metrics.setYellow("70.0");
        PDFOutput.setBackgroundColorForCell(cell, metrics, "80.0");
        assertEquals (Color.PINK, cell.getBackgroundColor());
    }

    @Test
    void testAddHeader() {
		final List<Integer> colWidths = new ArrayList<>();
		final PdfPTable table = new PdfPTable(3);
		final Config config = new Config();
		final SQMetrics metric1 = new SQMetrics();
        metric1.setTitle("Metric1");
		final SQMetrics metric2 = new SQMetrics();
        metric2.setTitle("Metric2");
        config.setMetrics(new SQMetrics[] {metric1, metric2});
        PDFOutput.addHeader(config,  table, colWidths);
        assertEquals (3, colWidths.size());
        assertEquals (2, colWidths.getFirst());
        assertEquals (15, colWidths.get(1));
        assertEquals (15, colWidths.get(2));
    }

    @Test
    void testClosePDF() {
		final Document document = new Document();
        PDFOutput.closePDF(document);
		assertFalse(document.isOpen());
    }

    @Test
    void testAddTextDashboardBody_setsGreenBackgroundAndUpdatesWidths() {
		final Config config = new Config();
		final SQMetrics metric1 = new SQMetrics();
        metric1.setTitle("MetricOne");
        metric1.setGreen("10");
        metric1.setYellow("5");
        config.setMetrics(new SQMetrics[] {metric1});
        Application app = new Application();
        app.setTitle("AppOne");
        config.setApplications(new Application[] { app });

        // prepare table and widths as addHeader would
        PdfPTable table = new PdfPTable(2); // metrics + project column
        List<Integer> colWidths = new ArrayList<>();
        colWidths.add(2); // project column initial
        colWidths.add(PDFOutput.getWidthOfString("MetricOne")); // metric column initial

        HashBasedTable<String, String, Double> data = HashBasedTable.create();
        data.put("MetricOne", "AppOne", 12.0); // will be formatted as "12"

        PDFOutput.addTextDashboardBody(config, table, data, colWidths);

        // verify table rows and cells
        ArrayList<PdfPRow> rows = table.getRows();
        // one application => one row
        assertEquals(1, rows.size());
        PdfPRow row = rows.get(0);
        PdfPCell[] cells = row.getCells();
        assertEquals(2, cells.length);

        // numeric cell should have been colored GREEN because 12 > green(10)
        PdfPCell numericCell = cells[1];
        Color bg = numericCell.getBackgroundColor();
        assertNotNull(bg);
        assertEquals(Color.GREEN, bg);

        // widths: first column should be at least width of "AppOne"
        int expectedAppWidth = PDFOutput.getWidthOfString("AppOne");
        int actualFirst = colWidths.get(0);
        assertEquals(expectedAppWidth, actualFirst);
    }

    @Test
    void testAddTextDashboardBody_setsYellowBackground_whenGreenLower() {
		final Config config = new Config();
		final SQMetrics metric1 = new SQMetrics();
        metric1.setTitle("MetricTwo");
        metric1.setGreen("10");
        metric1.setYellow("5");
        config.setMetrics(new SQMetrics[] {metric1});
        Application app = new Application();
        app.setTitle("AppTwo");
        config.setApplications(new Application[] { app });

        PdfPTable table = new PdfPTable(2); // metrics + project column
        List<Integer> colWidths = new ArrayList<>();
        colWidths.add(2);
        colWidths.add(PDFOutput.getWidthOfString("MetricTwo"));

        HashBasedTable<String, String, Double> data = HashBasedTable.create();
        data.put("MetricTwo", "AppTwo", 7.0); // between green(5) and yellow(10) -> should be YELLOW

        PDFOutput.addTextDashboardBody(config, table, data, colWidths);

        ArrayList<PdfPRow> rows = table.getRows();
        assertEquals(1, rows.size());
        PdfPRow row = rows.get(0);
        PdfPCell[] cells = row.getCells();
        assertEquals(2, cells.length);

        PdfPCell numericCell = cells[1];
        Color bg = numericCell.getBackgroundColor();
        assertNotNull(bg);
        assertEquals(Color.YELLOW, bg);
    }

    @Test
    void testAddTextDashboard() {
		final Config config = new Config();
		final SQMetrics metric1 = new SQMetrics();
        metric1.setTitle("MetricTwo");
        metric1.setGreen("10");
        metric1.setYellow("5");
        config.setMetrics(new SQMetrics[] {metric1});
        Application app = new Application();
        app.setTitle("AppTwo");
        config.setApplications(new Application[] { app });

        Document document = mock(Document.class);
        HashBasedTable<String, String, Double> data = HashBasedTable.create();
        data.put("MetricTwo", "AppTwo", 7.0);
        PDFOutput.addTextDashboard(document, data, config);
        verify(document, times(2)).add(any());
    }


}
