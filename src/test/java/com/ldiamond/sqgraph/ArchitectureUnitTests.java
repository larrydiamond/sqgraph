package com.ldiamond.sqgraph;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;

@AnalyzeClasses(packages = "com.ldiamond")
public class ArchitectureUnitTests {
    
    @ArchTest
    static final ArchRule no_vectors = noClasses().should().callConstructor(java.util.Vector.class).because("Use ArrayList");
    
    @ArchTest
    static final ArchRule no_hashtable = noClasses().should().callConstructor(java.util.Hashtable.class).because("Use HashSet");
    
    @ArchTest
    static final ArchRule no_stringbuffer = noClasses().should().callConstructor(java.lang.StringBuffer.class).because("Use StringBuilder");
    
    @ArchTest
    static final ArchRule no_treeset = noClasses().should().callConstructor(java.util.TreeSet.class).because("Use ConcurrentSkipListSet");

    @ArchTest
    static final ArchRule no_treemap = noClasses().should().callConstructor(java.util.TreeMap.class).because("Use ConcurrentSkipListMap");

    @ArchTest
    private final ArchRule no_jodatime = NO_CLASSES_SHOULD_USE_JODATIME;

}
