package com.ldiamond.sqgraph;

import lombok.Data;

@Data
public class Config {
    String url;
    String[] keys;
    SQMetrics[] metrics;
    int maxReportHistory;
}

@Data
class SQMetrics {
    String metric;
    String filename;
    String title;
}
