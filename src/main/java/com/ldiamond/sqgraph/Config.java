package com.ldiamond.sqgraph;

import lombok.Data;

@Data
public class Config {
    String url;
    String[] keys;
    String[] metrics;
    int maxReportHistory;
}
