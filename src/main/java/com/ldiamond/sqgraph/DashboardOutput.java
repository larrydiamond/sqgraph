package com.ldiamond.sqgraph;

import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.FontMetrics;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;

import com.google.common.collect.HashBasedTable;

public class DashboardOutput {

    private DashboardOutput () {}
    
    public static BufferedImage outputDashboard (HashBasedTable<String, String, Double> dashboardData, Config config) {
        try {
            String [] dashboardColumns = new String [1 + dashboardData.rowKeySet().size()];
            dashboardColumns [0] = "";
            int dcOffset = 1;
            for (String dcCol : dashboardData.rowKeySet()) {
                dashboardColumns [dcOffset++] = dcCol + " ";
            }

            String [] [] dashboardFormattedData = new String [config.getApplications().length] [];

            int rowLoop = 0;
            for (Application app : config.getApplications()) {
                Map<String,Double> rowMap = dashboardData.column(app.getTitle());
                String [] dRow = new String [1 + dashboardData.rowKeySet().size()];
                dRow [0] = " " + app.getTitle();
                int colLoop = 1;
                for (String dcCol : dashboardData.rowKeySet()) {
                    dRow [colLoop] = SqgraphApplication.standardDecimalFormatter.format (rowMap.get(dcCol)) + " ";
                    colLoop++;
                }
                dashboardFormattedData[rowLoop] = dRow;
                rowLoop++;
            }

            JTable jt = new JTable(dashboardFormattedData, dashboardColumns);
            sizeColumnsToFit(jt, 4);
            jt.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            jt.setGridColor(new Color(115,52,158));
            jt.setRowMargin(5);
            jt.setShowGrid(true);
            jt.doLayout();

            JScrollPane scroll = new JScrollPane(jt);
            JPanel p = new JPanel(new BorderLayout());
            p.add(scroll,BorderLayout.CENTER);

            p.addNotify();
            p.setSize(getTableWidth(jt, config) + 5, getTableHeight(jt));
            p.validate();

            BufferedImage bi = new BufferedImage(
                (int)p.getSize().getWidth(),
                (int)p.getSize().getHeight(),
                BufferedImage.TYPE_INT_RGB
                );

            Graphics g = bi.createGraphics();
            p.paint(g);

            int scaledWidth = Math.min(800, (int)p.getSize().getWidth());
            int scaledHeight = (((int)p.getSize().getHeight()) * scaledWidth) / ((int)p.getSize().getWidth());

            java.awt.Image scaled = bi.getScaledInstance(scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
            outputImage.getGraphics().drawImage(scaled, 0, 0, null);
            
            if (config.getDashboard() == null)
                config.setDashboard("dashboard.png");
        
            if (!config.getDashboard().endsWith(".png"))
                config.setDashboard(config.getDashboard() + ".png");

            ImageIO.write(bi,"png",new File(config.getDashboard()));
            g.dispose();

            return outputImage;

        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }

    }

    public static void sizeColumnsToFit(JTable table, int columnMargin) {
        JTableHeader tableHeader = table.getTableHeader();
        if(tableHeader == null) {
            // can't auto size a table without a header
            return;
        }
 
        FontMetrics headerFontMetrics = tableHeader.getFontMetrics(tableHeader.getFont());
 
        int[] minWidths = new int[table.getColumnCount()];
        int[] maxWidths = new int[table.getColumnCount()];
 
        for(int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
            int headerWidth = headerFontMetrics.stringWidth(table.getColumnName(columnIndex));
            minWidths[columnIndex] = headerWidth + columnMargin;
            int maxWidth = getMaximalRequiredColumnWidth(table, columnIndex, headerWidth);
            maxWidths[columnIndex] = Math.max(maxWidth, minWidths[columnIndex]) + columnMargin;
        }
 
        adjustMaximumWidths(table, minWidths, maxWidths);
        for(int i = 0; i < minWidths.length; i++) {
            if(minWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setMinWidth(minWidths[i]);
            }
 
            if(maxWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setMinWidth(maxWidths[i]);
                table.getColumnModel().getColumn(i).setMaxWidth(maxWidths[i]);
                table.getColumnModel().getColumn(i).setWidth(maxWidths[i]);
            }
        }
    }
 
    private static void adjustMaximumWidths(JTable table, int[] minWidths, int[] maxWidths) {
        if(table.getWidth() > 0) {
            // to prevent infinite loops in exceptional situations
            int breaker = 0;

            // sum of maxWidths is that stream thing below
            // keep stealing one pixel of the maximum width of the highest column until we can fit in the width of the table
            while((Arrays.stream(maxWidths).sum()) > table.getWidth() && breaker < 10000) {
                int highestWidthIndex = findLargestIndex(maxWidths);
                maxWidths[highestWidthIndex] -= 1;
                maxWidths[highestWidthIndex] = Math.max(maxWidths[highestWidthIndex], minWidths[highestWidthIndex]);
                breaker++;
            }
        }
    }
 
    private static int getMaximalRequiredColumnWidth(JTable table, int columnIndex, int headerWidth) {
        int maxWidth = headerWidth;
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        TableCellRenderer cellRenderer = column.getCellRenderer();
        if(cellRenderer == null) {
            cellRenderer = new DefaultTableCellRenderer();
        }
 
        for(int row = 0; row < table.getModel().getRowCount(); row++) {
            Component rendererComponent = cellRenderer.getTableCellRendererComponent(table,
                table.getModel().getValueAt(row, columnIndex),
                false,
                false,
                row,
                columnIndex);
 
            double valueWidth = rendererComponent.getPreferredSize().getWidth();
            maxWidth = (int) Math.max(maxWidth, valueWidth);
        }
 
        return maxWidth;
    }
 

    public static int findLargestIndex(int[] widths) {
        int largestIndex = 0;
        int largestValue = 0;
 
        for(int i = 0; i < widths.length; i++) {
            if(widths[i] > largestValue) {
                largestIndex = i;
                largestValue = widths[i];
            }
        }
 
        return largestIndex;
    }

	public static int getTableWidth (final JTable table, final Config config) {
		int width = 0;
		DashboardCellRenderer dcr = new DashboardCellRenderer(config);

		for(int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
			width += table.getColumnModel().getColumn(columnIndex).getMaxWidth();
			if (columnIndex > 0)
				table.getColumnModel().getColumn(columnIndex).setCellRenderer(dcr);
		}

		return width;
	}

	public static int getTableHeight (final JTable table) {
		int height = table.getTableHeader().getHeight();

		for(int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
			height += table.getRowHeight(rowIndex);
		}

		return Math.max(height, (2 + table.getRowCount()) * table.getRowHeight());
	}

}
