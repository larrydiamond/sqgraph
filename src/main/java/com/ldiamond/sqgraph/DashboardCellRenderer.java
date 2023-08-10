// Copyright Larry Diamond 2023 All Rights Reserved
package com.ldiamond.sqgraph;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class DashboardCellRenderer extends DefaultTableCellRenderer {
	final Config config;

	DashboardCellRenderer (final Config config) {
		this.config = config;
	}


	private Component greenHighThenYellowThenRed (double green, double yellow, JTable jt, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex, double cellvalue) {
		if (cellvalue > green) {
            Component dtcr = super.getTableCellRendererComponent(jt, value, isSelected, hasFocus, rowIndex, columnIndex);
            this.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
	        dtcr.setBackground(Color.green);
        	return dtcr;
		} else {
			if (cellvalue > yellow) {
                Component dtcr = super.getTableCellRendererComponent(jt, value, isSelected, hasFocus, rowIndex, columnIndex);
                this.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                dtcr.setBackground(Color.yellow);
                return dtcr;
			} else {
                Component dtcr = super.getTableCellRendererComponent(jt, value, isSelected, hasFocus, rowIndex, columnIndex);
                this.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                dtcr.setBackground(Color.pink);
                 return dtcr;
			}
		}
	}

	private Component greenLowThenYellowThenRed (double green, double yellow, JTable jt, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex, double cellvalue) {
		if (cellvalue < green) {
            Component dtcr = super.getTableCellRendererComponent(jt, value, isSelected, hasFocus, rowIndex, columnIndex);
            this.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
	        dtcr.setBackground(Color.green);
            return dtcr;
		} else {
			if (cellvalue < yellow) {
                Component dtcr = super.getTableCellRendererComponent(jt, value, isSelected, hasFocus, rowIndex, columnIndex);
                this.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                dtcr.setBackground(Color.yellow);
                return dtcr;
			} else {
                Component dtcr = super.getTableCellRendererComponent(jt, value, isSelected, hasFocus, rowIndex, columnIndex);
                this.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                dtcr.setBackground(Color.pink);
                return dtcr;
			}
		}
	}

	@Override public Component getTableCellRendererComponent(JTable jt, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
		SQMetrics metric = config.getMetrics()[columnIndex-1];
		String greenString = metric.getGreen();
		String yellowString = metric.getYellow();
		if ((greenString != null) && (yellowString != null)) {
			double green = Double.parseDouble(greenString);
			double yellow = Double.parseDouble(yellowString);
			boolean greenHigher = true;
			if (yellow > green) greenHigher = false;
			String cellvalueString = (String) jt.getModel().getValueAt(rowIndex, columnIndex);
			double cellvalue = Double.parseDouble(cellvalueString);
			if (greenHigher) {
				return greenHighThenYellowThenRed(green, yellow, jt, cellvalueString, isSelected, hasFocus, rowIndex, columnIndex, cellvalue);
			} else {
				return greenLowThenYellowThenRed(green, yellow, jt, cellvalueString, isSelected, hasFocus, rowIndex, columnIndex, cellvalue);
			}
		}
        
        Component dtcr = super.getTableCellRendererComponent(jt, value, isSelected, hasFocus, rowIndex, columnIndex);
        this.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        dtcr.setBackground(Color.white);
        return dtcr;
	}



}

