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
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
//import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
//import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "com.ldiamond.sqgraph")
class ArchitectureUnitTests {
       
    @Test
    void runArchitectureTests() {
        ArchitectureUnitTest.testArchitecture(
            Arrays.asList(ArchitectureRule.ARCHUNIT_DEPRECATED_API_SHOULD_NOT_BE_USED, ArchitectureRule.ARCHUNIT_NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS),
            "com.ldiamond.sqgraph");
    }

//    @ArchTest
//    static final ArchRule no_public_static_decimalformat =
//            noFields().that().arePublic().and().areStatic().should().haveRawType("java.text.DecimalFormat").because("DecimalFormat aint threadsafe use java.text.NumberFormatter").allowEmptyShould(true);

    @ArchTest
    static final ArchRule simpledateformat =
            noFields().that().arePublic().and().areStatic().should().haveRawType("java.text.SimpleDateFormat").because("SimpleDateFormat aint threadsafe use java.text.DateTimeFormatter").allowEmptyShould(true);


    @ArchTest
    static final ArchRule methods_with_the_async_annotation_must_be_public =
            methods().that().areAnnotatedWith("org.springframework.scheduling.annotation.Async").should().bePublic()
                    .because("Spring requires Async methods to be public").allowEmptyShould(true);

}
