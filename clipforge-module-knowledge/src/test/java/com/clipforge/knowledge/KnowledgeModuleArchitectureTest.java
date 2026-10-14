package com.clipforge.knowledge;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.clipforge.knowledge")
class KnowledgeModuleArchitectureTest {

    @ArchTest
    static final ArchRule knowledgeShouldNotDependOnLegacyOrMessaging =
            noClasses()
                    .that().resideInAnyPackage("com.clipforge.knowledge..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.clipforge.application..",
                            "com.clipforge.api..",
                            "com.clipforge.messaging..");
}
