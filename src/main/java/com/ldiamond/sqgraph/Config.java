// Copyright Larry Diamond 2023 All Rights Reserved
package com.ldiamond.sqgraph;

import lombok.Data;

@Data
public class Config {
    String url;
    Application[] applications;
    SQMetrics[] metrics;
    int maxReportHistory;
    String pdf;
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
