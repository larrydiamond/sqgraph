package com.ldiamond.sqgraph;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.ldiamond")
public class ArchitectureUnitTests {
    
    @ArchTest
    static final ArchRule no_vectors = noClasses().should().callConstructor(java.util.Vector.class);

}
