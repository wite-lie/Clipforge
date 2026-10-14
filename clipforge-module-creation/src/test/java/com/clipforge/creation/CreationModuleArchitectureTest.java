package com.clipforge.creation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.clipforge.creation")
class CreationModuleArchitectureTest {

    @ArchTest
    static final ArchRule creationBoundaryShouldNotDependOnLegacyImplementation =
            noClasses()
                    .that().resideInAnyPackage("com.clipforge.creation..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.clipforge.application..",
                            "com.clipforge.api..",
                            "com.clipforge.messaging..",
                            "com.clipforge.infra..",
                            "com.clipforge.rag..",
                            "com.clipforge.agent..",
                            "com.clipforge.material..");
}
