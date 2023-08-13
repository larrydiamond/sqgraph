/**
 *    Copyright (C) 2023-present Larry Diamond, All Rights Reserved.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    license linked below for more details.
 *
 *    For license terms, see <https://github.com/larrydiamond/sqgraph/blob/main/LICENSE.md>.
 **/
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

