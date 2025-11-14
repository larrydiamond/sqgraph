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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PDFOutput {
	private static final String standardDecimalFormat = "###,###,###.###";
	private static final DecimalFormat standardDecimalFormatter = new DecimalFormat (standardDecimalFormat);

    public static Document createPDF (final Config config) {
        Document document = null;
        try {
            document = new Document(new Rectangle(1800, (1300 * config.getMetrics().length)));
            PdfWriter.getInstance(document, new FileOutputStream(config.getPdf()));
            document.open();
            document.addTitle ("Management Code Metrics");
            final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm:ss a");
            final Paragraph paragraph = new Paragraph("Created by Management Code Metrics - CodeQualityGraph.com - " + sdf.format(LocalDateTime.now()));
            paragraph.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return document;
    }

	public static Document addGraphs(final Document document, final Config config) {
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
        return document;
	}

	public static Document closePDF(final Document document) {
        document.close();
        return document;
	}

    @VisibleForTesting
    static void addHeader(final Config config, final PdfPTable table, final List<Integer> colWidths) {
		final Phrase pphrase = new Phrase("Project");
		final Font pfont = pphrase.getFont();
        pfont.setSize(20);
        PdfPCell cell = new PdfPCell(pphrase);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setPaddingBottom(cell.getPaddingBottom() + 3);
        table.addCell(cell);
        colWidths.add(2);
        for (SQMetrics m : config.getMetrics()) {
			final Phrase phrase = new Phrase(m.getTitle());
			final Font font = phrase.getFont();
            font.setSize(20);
            cell = new PdfPCell(phrase);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
            colWidths.add(getWidthOfString(m.getTitle()));
        }
    }

    @VisibleForTesting
    static void addTextDashboardBody(final Config config, final PdfPTable table, final HashBasedTable<String, String, Double> dashboardData, final List<Integer> colWidths) {
        for (Application a : config.getApplications()) {
            int col = 0;
			final Phrase tphrase = new Phrase(a.getTitle());
			final Font tfont = tphrase.getFont();
            tfont.setSize(20);
            PdfPCell cell = new PdfPCell(tphrase);
            cell.setPaddingLeft(cell.getPaddingLeft() + 2);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setVerticalAlignment(Element.ALIGN_CENTER);
            cell.setPaddingBottom(cell.getPaddingBottom() + 3);
            setMax(colWidths, col, a.getTitle(), 0);
            table.addCell(cell);
            for (SQMetrics m : config.getMetrics()) {
				final String text = standardDecimalFormatter.format(dashboardData.get(m.getTitle(), a.getTitle()));
				final Phrase phrase = new Phrase(text);
				final Font font = phrase.getFont();
                font.setSize(20);
                cell = new PdfPCell(phrase);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell.setVerticalAlignment(Element.ALIGN_CENTER);
                cell.setPaddingLeft(cell.getPaddingLeft() + 2);
                cell.setPaddingRight(cell.getPaddingRight() + 5);
				final SQMetrics metric = config.getMetrics()[col];

                setBackgroundColorForCell(cell, metric, text);

                table.addCell(cell);
                col++;
                setMax(colWidths, col, text, 4);
            }
        }
    }

    public static Document addTextDashboard(final Document document, final HashBasedTable<String, String, Double> dashboardData, final Config config) {
        try {
            document.add(new Phrase("")); // spacer

			final List<Integer> colWidths = new ArrayList<>();
			final PdfPTable table = new PdfPTable(config.getMetrics().length + 1);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.setWidthPercentage(95);
            table.setSpacingBefore(2);
            table.setSpacingAfter(2);
            addHeader(config, table, colWidths);
            addTextDashboardBody(config, table, dashboardData, colWidths);

			final int[] w = new int[colWidths.size()];
			final int colWidthsSize = colWidths.size();
            for (int loop = 0; loop < colWidthsSize; loop++) {
                w[loop] = colWidths.get(loop);
            }

            table.setWidths(w);
            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return document;
    }

    static void setBackgroundColorForCellGreenHigher(final double cellValue, final double green, final double yellow, final PdfPCell cell) {
        if (cellValue > green) {
            cell.setBackgroundColor(Color.GREEN);
        } else {
            if (cellValue > yellow) {
                cell.setBackgroundColor(Color.YELLOW);
            } else {
                cell.setBackgroundColor(Color.PINK);
            }
        }
    }

    static void setBackgroundColorForCellGreenLower(final double cellValue, final double green, final double yellow, final PdfPCell cell) {
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

    @VisibleForTesting
    static void setBackgroundColorForCell (final PdfPCell cell, final SQMetrics metric, final String text) {
		final String greenString = metric.getGreen();
		final String yellowString = metric.getYellow();
        if ((greenString != null) && (yellowString != null)) {
			final double green = Double.parseDouble(greenString);
			final double yellow = Double.parseDouble(yellowString);
            boolean greenHigher = true;
            if (yellow > green) 
                greenHigher = false;
			final double cellValue = Double.parseDouble(text);
            if (greenHigher) {
                setBackgroundColorForCellGreenHigher(cellValue, green, yellow, cell);
            } else {
                setBackgroundColorForCellGreenLower(cellValue, green, yellow, cell);
            }
        }
    }

    @VisibleForTesting
    static int getWidthOfString(final String s) {
        int width = 2;

        for (int offset = 0; offset < s.length(); offset++) {
			final char c = s.charAt(offset);
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

    @VisibleForTesting
    static void setMax (List<Integer> widths, int col, String s, int plus) {
		final int width = getWidthOfString(s) + plus;
		final int curMax = widths.get(col);
        if (width > curMax) {
            widths.set (col, width);
        }
    }
}
