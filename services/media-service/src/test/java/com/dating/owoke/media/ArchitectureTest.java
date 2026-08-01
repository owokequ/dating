package com.dating.owoke.media;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.dating.owoke.media")
class ArchitectureTest {

    @ArchTest
    static final ArchRule DOMAIN_IS_INDEPENDENT = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..controller..", "..dto..", "..service..", "..repository..", "..mapper..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule REPOSITORIES_POINT_INWARD = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..controller..", "..dto..", "..service..", "..mapper..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule SERVICES_DO_NOT_DEPEND_ON_CONTROLLERS = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .allowEmptyShould(true);
}
