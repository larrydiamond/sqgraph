package com.ldiamond.sqgraph;

import lombok.Data;

@Data
public class MetricsResults {
    Metric [] metrics;    
}

@Data
class Metric {
    String key;
    String type;
    String name;
    String description;
    String domain;
    String qualitative;
    String hidden;
}

