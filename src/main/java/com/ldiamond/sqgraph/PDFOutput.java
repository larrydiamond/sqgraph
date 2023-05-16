package com.ldiamond.sqgraph;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Table;
import com.lowagie.text.alignment.HorizontalAlignment;
import com.lowagie.text.pdf.PdfWriter;

public class PDFOutput {
    
    private PDFOutput () {}

    static Document document = null;

    public static void createPDF (final Config config) {
        try {
            document = new Document(new Rectangle(900, (700 * config.getMetrics().length)));
            PdfWriter.getInstance(document, new FileOutputStream(config.getPdf()));
            document.open();
            document.addTitle ("Management Code Metrics");
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm aa");
            final Paragraph paragraph = new Paragraph("Created by Management Code Metrics - CodeQualityGraph.com - " + sdf.format(new Date()));
            paragraph.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void addDashboard(final BufferedImage bi) {
        try {
            Image png = Image.getInstance(bi, null);
            document.add(png);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

	public static void addGraphs(final Config config) {
        try {
            for (SQMetrics sqm : config.getMetrics()) {
                final Image png = Image.getInstance(sqm.getFilename());
                if (png != null) {
                    if (sqm.getDescription() != null)
                        document.add(new Paragraph("\n" + sqm.getDescription()));
                    document.add(png);
                }

            }    
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
	}

	public static void closePDF() {
        document.close();
	}

    private static int getWidthOfString(final String s) {
        int width = 2;

        for (int offset = 0; offset < s.length(); offset++) {
            char c = s.charAt(offset);
            if ((c == 'i') || (c == 'l') || (c == 't') || (c == '.') || (c == ',')) {
                width++;
            } else {
                if (Character.isUpperCase(c)) {
                    width += 3;
                } else {
                    width += 2;
                }
            }
        }

        return width;
    }

    private static void setMax (List<Integer> widths, int col, String s) {
        int width = getWidthOfString(s);
        int curMax = widths.get (col);
        if (width > curMax) {
            widths.set (col, width);
        }
    }

    public static void addTextDashboard(final HashBasedTable<String, String, Double> dashboardData, final Config config) {
        List<Integer> colWidths = new ArrayList<>(); 
        
        Table table = new Table(config.getMetrics().length + 1);
        table.setWidth(100);
        table.setPadding(3);
        table.setSpacing(1);
        Cell cell = new Cell("");
        cell.setHeader(true);
        table.addCell(cell);
        colWidths.add (2);
        int col = 1;
        for (SQMetrics m : config.getMetrics()) {
            cell = new Cell(m.getTitle());
            cell.setHorizontalAlignment(HorizontalAlignment.CENTER);
            cell.setHeader(true);
            table.addCell(cell);
            colWidths.add(getWidthOfString (m.getTitle()));
            col++;
        }
        table.endHeaders();
        for (Application a : config.getApplications()) {
            col = 0;
            cell = new Cell(a.getTitle());
            cell.setHorizontalAlignment(HorizontalAlignment.LEFT);
            setMax(colWidths, col, a.getTitle());
            table.addCell(cell);
            for (SQMetrics m : config.getMetrics()) {
                col++;
                String text = SqgraphApplication.standardDecimalFormatter.format (dashboardData.get(m.getTitle(),a.getTitle()));
                cell = new Cell(text);
                cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                table.addCell(cell);
                setMax(colWidths, col, text);
            }
        }

        int [] w = new int [colWidths.size()];
        for (int loop = 0; loop < colWidths.size(); loop++) {
            System.out.println ("width " + loop + " = " + colWidths.get(loop));
            w [loop] = colWidths.get(loop);
        }

        table.setWidths(w);

        document.add (table);
    }
}
