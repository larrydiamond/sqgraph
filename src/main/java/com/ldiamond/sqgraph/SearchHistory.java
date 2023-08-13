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