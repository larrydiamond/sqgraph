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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;

class PDFOutputTests {

    @Test
    void testAddNoGraphsReturnsSameDocument() {
        Document document = new Document(new Rectangle(1800, (1400 * 10)));
        Config config = new Config();
        config.setMetrics(new SQMetrics[0]);
        Document resultDocument = PDFOutput.addGraphs(document, config);
        assertEquals (document, resultDocument);
    }

    @Test
    void testGetWidthOfStringForBlah() {
        int width = PDFOutput.getWidthOfString("Blah");
        assertEquals (10, width);
    }

    @Test 
    void testSetMax() {
        List<Integer> widths = new ArrayList<>();
        widths.add(5);
        widths.add(7000);
        PDFOutput.setMax(widths, 0, "Blah", 10);
        PDFOutput.setMax(widths, 1, "Blah", 10);
        assertEquals (20, widths.get(0));
        assertEquals (7000, widths.get(1));
    }

    @Test
    void TestSetBackgroundColorForCellNoColor() {
        PdfPCell cell = new PdfPCell();
        SQMetrics metrics = new SQMetrics();
        PDFOutput.setBackgroundColorForCell(cell, metrics, "60.0");
        assertEquals (null, cell.getBackgroundColor());
    }

    @Test
    void TestSetBackgroundColorForCellHigherGreen() {
        PdfPCell cell = new PdfPCell();
        SQMetrics metrics = new SQMetrics();
        metrics.setGreen("70.0");
        metrics.setYellow("50.0");
        PDFOutput.setBackgroundColorForCell(cell, metrics, "80.0");
        assertEquals (Color.GREEN, cell.getBackgroundColor());
    }
}
