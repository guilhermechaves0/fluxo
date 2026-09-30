package br.ufrn.fluxo.arquitetura

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test
import kotlin.test.assertTrue

private const val RAIZ = "br.ufrn.fluxo"
private const val DOMINIO = "$RAIZ.dominio.."
private const val APLICACAO = "$RAIZ.aplicacao.."
private const val ADAPTADORES = "$RAIZ.adaptadores.."
private const val VIOLACAO = "$RAIZ.arquitetura.violacao"

/**
 * A regra de dependência entre as camadas, conferida pelo ArchUnit no código compilado.
 *
 *     adaptadores  ->  aplicacao  ->  dominio
 *     (Ktor, Koin)     (casos de uso    (Kotlin puro,
 *                       e portas)        em shared/)
 *
 * As dependências só apontam para dentro. O domínio vem de `shared/`, que nem tem Ktor entre as
 * dependências. A camada de aplicação fica neste módulo, ao lado dos frameworks, e só este teste
 * impede que um caso de uso passe a conhecer HTTP, JSON ou banco. Ele roda no CI com os outros
 * testes da api.
 */
class ArquiteturaTest {
    private val classes: JavaClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages(RAIZ)

    @Test
    fun dominioSoDependeDeKotlinEDoTipoDeData() = regraDoDominio(DOMINIO).check(classes)

    @Test
    fun aplicacaoNaoConheceFrameworkNemInfraestrutura() = regraDaAplicacao(APLICACAO).check(classes)

    @Test
    fun aplicacaoNaoDependeDosAdaptadores() = noClasses()
        .that()
        .resideInAPackage(APLICACAO)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ADAPTADORES)
        .check(classes)

    // As duas classes de `violacao/` existem só nos testes e quebram as regras de propósito. Se um
    // dia uma regra passar com elas, a regra deixou de proteger a camada.

    @Test
    fun aRegraDoDominioAcusaQuemImportaFramework() {
        val contaminadas = ClassFileImporter().importPackages(VIOLACAO)
        val resultado = regraDoDominio("$VIOLACAO.dominio..").evaluate(contaminadas)
        assertTrue(resultado.hasViolation(), "a regra do domínio deveria acusar o import de Ktor")
    }

    @Test
    fun aRegraDaAplicacaoAcusaQuemImportaFramework() {
        val contaminadas = ClassFileImporter().importPackages(VIOLACAO)
        val resultado = regraDaAplicacao("$VIOLACAO.aplicacao..").evaluate(contaminadas)
        assertTrue(resultado.hasViolation(), "a regra da aplicação deveria acusar o import de Ktor")
    }
}

/** O domínio só usa a biblioteca padrão e o `kotlinx.datetime`, de onde vem o tipo de data. */
private fun regraDoDominio(pacote: String): ArchRule = classes()
    .that()
    .resideInAPackage(pacote)
    .should()
    .onlyDependOnClassesThat()
    .resideInAnyPackage(pacote, "kotlin..", "java..", "kotlinx.datetime..", "org.jetbrains.annotations..")
    .because("o domínio é Kotlin puro e compila para o app e para a api")

private fun regraDaAplicacao(pacote: String): ArchRule = noClasses()
    .that()
    .resideInAPackage(pacote)
    .should()
    .dependOnClassesThat()
    .resideInAnyPackage("io.ktor..", "org.koin..", "kotlinx.serialization..", "java.sql..", "javax.sql..")
    .because("casos de uso e portas não sabem de HTTP, de JSON, de banco nem de injeção de dependência")
