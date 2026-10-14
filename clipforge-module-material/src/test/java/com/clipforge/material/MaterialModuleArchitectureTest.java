package com.clipforge.material;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.clipforge.material")
class MaterialModuleArchitectureTest {

    @ArchTest
    static final ArchRule materialShouldNotDependOnLegacyOrMessaging =
            noClasses()
                    .that().resideInAnyPackage("com.clipforge.material..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.clipforge.application..",
                            "com.clipforge.api..",
                            "com.clipforge.messaging..",
                            "com.clipforge.infra..",
                            "com.clipforge.knowledge..",
                            "com.clipforge.rag..");
}
