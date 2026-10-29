package com.br.rkfbank;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

// Monolito modular: as fronteiras viram teste. Modulos por assunto (clientes, contas, autenticacao, titular) e o
// compartilhado (config, erros, web, validacao). Quebrou uma regra, o build fica vermelho com a classe e a linha.
@AnalyzeClasses(packages = "com.br.rkfbank", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaModulosTest {

    // O compartilhado e a base de todos: se ele dependesse de um modulo, todo modulo dependeria de todos.
    @ArchTest
    static final ArchRule compartilhadoNaoConheceOsModulos = noClasses()
            .that().resideInAPackage("..compartilhado..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..clientes..", "..contas..", "..autenticacao..", "..titular..")
            .because("o compartilhado e usado por todos os modulos e nao pode depender de nenhum");

    // Porta ConsultaCep: so o adaptador conhece o ViaCEP. A regra do cadastro fala com a interface.
    @ArchTest
    static final ArchRule soOAdaptadorConheceOViaCep = noClasses()
            .that().resideOutsideOfPackage("..clientes.viacep..")
            .should().dependOnClassesThat().resideInAPackage("..clientes.viacep..")
            .because("o ViaCEP fica atras da porta ConsultaCep (clientes.viacep.ViaCepConsultaCep)");

    // Camadas dentro de cada modulo: o controller so traduz HTTP e chama o service; nunca o banco.
    @ArchTest
    static final ArchRule controllerNaoFalaComRepository = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().areAssignableTo(Repository.class)
            .because("acesso a dados passa pelo service, que tem a regra e o @Transactional");

    // A entidade e o modelo do negocio: nao conhece o formato da API nem quem a chama.
    @ArchTest
    static final ArchRule entidadeNaoConheceDtoNemWeb = noClasses()
            .that().areAnnotatedWith(Entity.class)
            .should().dependOnClassesThat().resideInAnyPackage("..dto..", "org.springframework.web..")
            .because("a entidade nunca sai da API; quem converte para DTO e o service");

    // DTO so carrega dados: nao chama service nem banco.
    @ArchTest
    static final ArchRule dtoSoCarregaDados = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Service")
            .orShould().dependOnClassesThat().areAssignableTo(Repository.class);

    // Cada modulo e dono do seu banco: ninguem de fora usa o repository dele (nem como campo, parametro ou chamada).
    // A excecao e o titular (LGPD): encerrar e anonimizar o cadastro mexe, por definicao, em cliente, contas, extrato
    // e credencial.
    @ArchTest
    static final ArchRule repositoryDeContasSoNoModuloContas = noClasses()
            .that().resideOutsideOfPackages("..contas..", "..titular..")
            .should().dependOnClassesThat(resideInAPackage("..contas..").and(assignableTo(Repository.class)))
            .because("o banco do modulo contas so e usado por ele (e pelo titular, na LGPD)");

    @ArchTest
    static final ArchRule repositoryDeClientesSoNoModuloClientes = noClasses()
            .that().resideOutsideOfPackages("..clientes..", "..titular..")
            .should().dependOnClassesThat(resideInAPackage("..clientes..").and(assignableTo(Repository.class)))
            .because("o banco do modulo clientes so e usado por ele (e pelo titular, na LGPD)");

    @ArchTest
    static final ArchRule repositoryDeAutenticacaoSoNoModuloAutenticacao = noClasses()
            .that().resideOutsideOfPackages("..autenticacao..", "..titular..")
            .should().dependOnClassesThat(resideInAPackage("..autenticacao..").and(assignableTo(Repository.class)))
            .because("o banco do modulo autenticacao so e usado por ele (e pelo titular, na LGPD)");

    // Controllers moram no modulo do assunto deles, nunca no compartilhado.
    @ArchTest
    static final ArchRule controllersFicamNosModulos = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAnyPackage("..clientes", "..contas", "..autenticacao", "..titular");
}
