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

import java.awt.Color;
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
import com.lowagie.text.Font;
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
            document = new Document(new Rectangle(1800, (1400 * config.getMetrics().length)));
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

    public static void addTextDashboard(final HashBasedTable<String, String, Double> dashboardData, final Config config) {
        try {
            document.add(new Phrase("")); // spacer

            List<Integer> colWidths = new ArrayList<>(); 
            PdfPTable table = new PdfPTable(config.getMetrics().length + 1);
            table.setHorizontalAlignment(table.ALIGN_LEFT);
            table.setWidthPercentage(95);
            table.setSpacingBefore(2);
            table.setSpacingAfter(2);
            Phrase pphrase = new Phrase("Project");
            Font pfont = pphrase.getFont();
            pfont.setSize(20);
            PdfPCell cell = new PdfPCell(pphrase);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setPaddingBottom(cell.getPaddingBottom() + 3);
            table.addCell(cell);
            colWidths.add(2);
            int col = 1;
            for (SQMetrics m : config.getMetrics()) {
                Phrase phrase = new Phrase(m.getTitle());
                Font font = phrase.getFont();
                font.setSize(20);
                cell = new PdfPCell(phrase);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
                colWidths.add(getWidthOfString(m.getTitle()));
                col++;
            }
            for (Application a : config.getApplications()) {
                col = 0;
                Phrase tphrase = new Phrase(a.getTitle());
                Font tfont = tphrase.getFont();
                tfont.setSize(20);
                cell = new PdfPCell(tphrase);
                cell.setPaddingLeft(cell.getPaddingLeft() + 2);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell.setVerticalAlignment(Element.ALIGN_CENTER);
                cell.setPaddingBottom(cell.getPaddingBottom() + 3);
                setMax(colWidths, col, a.getTitle(), 0);
                table.addCell(cell);
                for (SQMetrics m : config.getMetrics()) {
                    String text = SqgraphApplication.standardDecimalFormatter.format(dashboardData.get(m.getTitle(), a.getTitle()));
                    Phrase phrase = new Phrase(text);
                    Font font = phrase.getFont();
                    font.setSize(20);
                    cell = new PdfPCell(phrase);
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cell.setVerticalAlignment(Element.ALIGN_CENTER);
                    cell.setPaddingLeft(cell.getPaddingLeft() + 2);
                    cell.setPaddingRight(cell.getPaddingRight() + 5);
                    SQMetrics metric = config.getMetrics()[col];
                    String greenString = metric.getGreen();
                    String yellowString = metric.getYellow();
            		if ((greenString != null) && (yellowString != null)) {
			            double green = Double.parseDouble(greenString);
			            double yellow = Double.parseDouble(yellowString);
			            boolean greenHigher = true;
			            if (yellow > green) greenHigher = false;
                        double cellValue = Double.parseDouble(text);
                        if (greenHigher) {
                            if (cellValue > green) {
                                cell.setBackgroundColor(Color.GREEN);
                            } else {
                                if (cellValue > yellow) {
                                    cell.setBackgroundColor(Color.YELLOW);
                                } else {
                                    cell.setBackgroundColor(Color.PINK);
                                }
                            }
                        } else {
                            if (cellValue < green) {
                                cell.setBackgroundColor(Color.GREEN);
                            } else {
                                if (cellValue < yellow) {
                                    cell.setBackgroundColor(Color.YELLOW);
                                } else {
                                    cell.setBackgroundColor(Color.PINK);
                                }
                            }
                        }
                    }
                    table.addCell(cell);
                    col++;
                    setMax(colWidths, col, text, 4);
                }
            }

            int[] w = new int[colWidths.size()];
            int colWidthsSize = colWidths.size();
            for (int loop = 0; loop < colWidthsSize; loop++) {
                w[loop] = colWidths.get(loop);
            }

            table.setWidths(w);
            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
