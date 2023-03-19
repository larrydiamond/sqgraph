package com.ldiamond.sqgraph;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

public class PDFOutput {
    
    private PDFOutput () {}

    static Document document = null;

    public static void createPDF (final Config config) {
        try {
            document = new Document(new Rectangle(900, 700));
            PdfWriter.getInstance(document, new FileOutputStream(config.getPdf()));
            document.open();
            document.addTitle ("Code Quality Graphs");
            final Paragraph paragraph = new Paragraph("Created by the Code Quality Graphing Tool");
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
                document.add(png);
            }    
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
	}

	public static void closePDF() {
        document.close();
	}

}
