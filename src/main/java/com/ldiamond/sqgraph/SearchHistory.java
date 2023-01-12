package com.ldiamond.sqgraph;

import java.util.Date;

import lombok.Data;

@Data
public class SearchHistory {
    Paging paging;    
    Measure [] measures;
}

@Data
class Paging {
    int pageIndex;
    int pageSize;
    int total;
}

@Data
class Measure {
    String metric;
    History [] history;
}

@Data 
class History {
    Date date;
    double value;
}