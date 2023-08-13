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

import java.io.Serializable;

import lombok.Data;

@Data
public class Config implements Serializable {
    private static final long serialVersionUID = 2405172041950251807L;
    String url;
    Application[] applications;
    SQMetrics[] metrics;
    int maxReportHistory;
    String pdf;
    String dashboard;
}

@Data
class SQMetrics implements Serializable {
    private static final long serialVersionUID = 2405111141950251807L;
    String metric;
    String filename;
    String title;
    String green;
    String yellow;
    String description;
}

@Data
class Application implements Serializable {
    private static final long serialVersionUID = 2422222041950251807L;
    String key;
    String title;
}
