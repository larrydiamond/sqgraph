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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
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
}
