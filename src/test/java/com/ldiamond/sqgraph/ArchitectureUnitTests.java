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
import org.junit.jupiter.api.Test;
import java.util.Arrays;

import com.ldiamond.archunittest.ArchitectureRule;
import com.ldiamond.archunittest.ArchitectureUnitTest;

public class ArchitectureUnitTests {
       
    @Test
    public void runArchitectureTests() {
        ArchitectureUnitTest.testArchitecture(
            Arrays.asList(ArchitectureRule.ARCHUNIT_DEPRECATED_API_SHOULD_NOT_BE_USED, ArchitectureRule.ARCHUNIT_NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS),
            "com.ldiamond.sqgraph");
    }

}
