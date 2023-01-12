package com.ldiamond.sqgraph;

import lombok.Data;

@Data
public class Config {
    String url;
    Application[] applications;
    SQMetrics[] metrics;
    int maxReportHistory;
}

@Data
class SQMetrics {
    String metric;
    String filename;
    String title;
}

@Data
class Application {
    String key;
    String title;
}
