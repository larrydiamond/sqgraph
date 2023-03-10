// Copyright Larry Diamond 2023 All Rights Reserved
package com.ldiamond.sqgraph;

import java.util.List;
import java.util.Map;

public interface SyntheticMetric {
    String getSyntheicName ();
    List<String> getRealMetrics();
    double calculate (Map<String,Double> metrics);
}
