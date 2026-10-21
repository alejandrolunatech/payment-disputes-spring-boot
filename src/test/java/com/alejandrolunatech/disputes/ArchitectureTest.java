package com.alejandrolunatech.disputes;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** The hexagonal layout, enforced: a change that breaks these rules fails the build. */
@AnalyzeClasses(packages = "com.alejandrolunatech.disputes", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsPlainJava = classes().that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..domain..", "java..")
            .because("the business rules must not depend on any framework");

    @ArchTest
    static final ArchRule applicationKnowsNoFrameworkOrAdapter = classes().that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "..application..", "java..", "io.github.resilience4j.circuitbreaker..")
            .because("use cases talk to the outside only through their ports; the circuit breaker is a plain library");

    @ArchTest
    static final ArchRule adaptersDoNotKnowEachOther = noClasses().that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");

    @ArchTest
    static final ArchRule outboundAdaptersStayApart = noClasses().that().resideInAPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out.llm..");

    @ArchTest
    static final ArchRule onlyTheLlmAdapterTouchesSpringAi = noClasses().that()
            .resideOutsideOfPackages("..adapter.out.llm..", "..config..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.ai..");
}
