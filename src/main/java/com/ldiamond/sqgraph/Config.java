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
    String dashboard;
}

@Data
class SQMetrics {
    String metric;
    String filename;
    String title;
    String green;
    String yellow;
}

@Data
class Application {
    String key;
    String title;
}
