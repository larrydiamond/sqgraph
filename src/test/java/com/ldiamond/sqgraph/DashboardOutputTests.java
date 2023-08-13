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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class DashboardOutputTests {

    int [] initalizeArray () {
        int [] array = new int [5];
        array [0] = 1;
        array [1] = 1;
        array [2] = 1;
        array [3] = 1;
        array [4] = 1;
        return array;
    }

    @Test void findLargestIndexFirstElement () {
        int [] array = initalizeArray();
        array [0] = 20;
        assertEquals (0, DashboardOutput.findLargestIndex(array));
    }

    @Test void findLargestIndexMiddleElement () {
        int [] array = initalizeArray();
        array [2] = 20;
        assertEquals (2, DashboardOutput.findLargestIndex(array));
    }

    @Test void findLargestIndexLastElement () {
        int [] array = initalizeArray();
        array [4] = 20;
        assertEquals (4, DashboardOutput.findLargestIndex(array));
    }

}
