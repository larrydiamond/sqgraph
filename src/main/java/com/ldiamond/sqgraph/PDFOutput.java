package com.ldiamond.sqgraph;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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

    private static void setMax (List<Integer> widths, int col, String s, int plus) {
        int width = getWidthOfString(s) + plus;
        int curMax = widths.get (col);
        if (width > curMax) {
            widths.set (col, width);
        }
    }

    public static void addTextDashboard(final HashBasedTable<String, String, Double> dashboardData, final Config config) {
        document.add(new Phrase ("")); // spacer
//        Phrase p = new Phrase ("blah");
//        Font f = p.getFont();
//        System.out.println ("Font " + f.getFamilyname() + " size " + f.getSize());

        List<Integer> colWidths = new ArrayList<>(); 
        
        PdfPTable table = new PdfPTable(config.getMetrics().length + 1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(1);
        table.setSpacingAfter(2);
        PdfPCell cell = new PdfPCell(new Phrase ("Text"));
        table.addCell(cell);
        colWidths.add (2);
        int col = 1;
        for (SQMetrics m : config.getMetrics()) {
            cell = new PdfPCell(new Phrase (m.getTitle()));

            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
            colWidths.add(getWidthOfString (m.getTitle()));
            col++;
        }
        for (Application a : config.getApplications()) {
            col = 0;
            cell = new PdfPCell(new Phrase (a.getTitle()));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setVerticalAlignment(Element.ALIGN_CENTER);
            setMax(colWidths, col, a.getTitle(), 0);
            table.addCell(cell);
            for (SQMetrics m : config.getMetrics()) {
                col++;
                String text = SqgraphApplication.standardDecimalFormatter.format (dashboardData.get(m.getTitle(),a.getTitle()));
                cell = new PdfPCell(new Phrase (text));
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell.setVerticalAlignment(Element.ALIGN_CENTER);
                cell.setPaddingLeft(cell.getPaddingLeft() + 2);
                cell.setPaddingRight(cell.getPaddingRight() + 2);
                table.addCell(cell);
                setMax(colWidths, col, text, 4);
            }
        }

        int [] w = new int [colWidths.size()];
        int sum = 0;
        int colWidthsSize = colWidths.size();
        for (int loop = 0; loop < colWidthsSize; loop++) {
//            System.out.println ("width " + loop + " = " + colWidths.get(loop));
            w [loop] = colWidths.get(loop);
            sum += w [loop];
        }
        System.out.println ("Sum = " + sum);

        

        table.setWidths(w);

        document.add (table);
    }
}
