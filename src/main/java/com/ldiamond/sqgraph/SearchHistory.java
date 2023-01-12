package com.ldiamond.sqgraph;

import java.util.Date;

import lombok.Data;

@Data
public class SearchHistory {
    Paging paging;    
    Measures [] measures;
}

@Data
class Paging {
    int pageIndex;
    int pageSize;
    int total;
}

@Data
class Measures {
    String metric;
    History [] history;
}

@Data 
class History {
    Date date;
    double value;
}