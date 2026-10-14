package com.clipforge.memory;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.clipforge.memory")
class MemoryModuleArchitectureTest {

    @ArchTest
    static final ArchRule memoryShouldNotDependOnLegacyOrMessaging =
            noClasses()
                    .that().resideInAnyPackage("com.clipforge.memory..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.clipforge.application..",
                            "com.clipforge.api..",
                            "com.clipforge.messaging..",
                            "com.clipforge.infra..",
                            "com.clipforge.rag..",
                            "com.clipforge.agent..");
}
