import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
// import org.gradle.plugins.signing.SigningExtension

plugins {
    id("java-gradle-plugin")
    id("jacoco")
    id("maven-publish")
    // id("signing")
    id("com.gradle.plugin-publish") version "2.1.1"
    id("org.owasp.dependencycheck") version "10.0.3"
    id("com.github.spotbugs") version "6.1.0"
    id("org.sonarqube") version "6.0.1.5171"
}

group = "name.jurgenei.gradle"
version = "0.1.8"

repositories {
    mavenCentral()
}

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

extensions.configure<GradlePluginDevelopmentExtension> {
    website.set("https://github.com/jurgenei/gradle-xml-plugin")
    vcsUrl.set("https://github.com/jurgenei/gradle-xml-plugin.git")

    plugins {
        create("xmlTransformPlugin") {
            id = "name.jurgenei.gradle.xml"
            implementationClass = "name.jurgenei.gradle.xml.XmlTransformPlugin"
            displayName = "XML Transform & Validate Plugin"
            description = "Saxon based XSLT, XQuery, Schematron and XSD tasks with SVRL/JUnit reporting"
            tags.set(
                listOf(
                    "xml",
                    "gradle-plugin",
                    "xslt",
                    "xquery",
                    "schematron",
                    "xsd",
                    "saxon",
                    "svrl",
                    "junit"
                )
            )
        }
    }
}

extensions.configure<PublishingExtension> {
    repositories {
        maven {
            name = "central"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = providers.gradleProperty("mavenCentralUsername").orNull
                password = providers.gradleProperty("mavenCentralPassword").orNull
            }
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Gradle XML Plugin")
            description.set("Gradle plugin for XSLT, XQuery, Schematron, and XSD validation")
            url.set("https://github.com/jurgenei/gradle-xml-plugin.git")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }

            developers {
                developer {
                    id.set("jurgenei")
                    name.set("Jurgenei")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/jurgenei/gradle-xml-plugin.git")
                developerConnection.set("scm:git:ssh://github.com/jurgenei/gradle-xml-plugin.git")
                url.set("https://github.com/jurgenei/gradle-xml-plugin")
            }
        }
    }
}

//signing {
//    useInMemoryPgpKeys(
//        providers.gradleProperty("signingKey").orNull,
//        providers.gradleProperty("signingPassword").orNull
//    )
//    sign(publishing.publications)
//}

//extensions.configure<SigningExtension> {
//    useGpgCmd()
//    sign(extensions.getByType(PublishingExtension::class.java).publications)
//}

// OWASP Dependency-Check configuration
extensions.getByName("dependencyCheck").withGroovyBuilder {
    setProperty("format", "HTML,JSON,XML")
    setProperty("failBuildOnCVSS", 7.0f)
    setProperty("suppressionFile", "dependency-check-suppressions.xml")

    // NVD API key configuration (improves scan speed by 30-50%)
    // Get key from: https://nvd.nist.gov/developers/request-an-api-key
    getProperty("nvd").withGroovyBuilder {
        setProperty(
            "apiKey",
            providers.gradleProperty("org.owasp.dependencycheck.nvd.api.key").orNull
                ?: System.getenv("NVD_API_KEY")
        )
    }
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    ignoreFailures = true
    effort = com.github.spotbugs.snom.Effort.DEFAULT
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
    reports.create("html").required.set(true)
    reports.create("xml").required.set(false)
}

// SonarQube configuration
extensions.getByName("sonar").withGroovyBuilder {
    "properties" {
        "property"("sonar.projectKey", "gradle-xml-plugin")
        "property"("sonar.projectName", "Gradle XML Plugin")
        "property"("sonar.sourceEncoding", "UTF-8")
        "property"("sonar.java.source", "21")
    }
}

dependencies {
    add("implementation", "net.sf.saxon:Saxon-HE:12.5")
    add("implementation", "name.dmaus.schxslt:schxslt2:1.10.3")

    add("testImplementation", gradleTestKit())
    add("testImplementation", "junit:junit:4.13.2")
}

tasks.named<Test>("test") {
    useJUnit()
    finalizedBy(tasks.named("jacocoTestReport"))
}

extensions.configure<JacocoPluginExtension> {
    toolVersion = "0.8.12"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.00".toBigDecimal()
            }
        }
    }
}

tasks.register("coverage") {
    group = "verification"
    description = "Runs tests, generates JaCoCo report, and verifies minimum coverage threshold."
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

tasks.register("allSecurityChecks") {
    group = "verification"
    description = "Run all security and quality checks (Dependency-Check, SpotBugs, SonarQube)"
    dependsOn("check", "dependencyCheck", "spotbugsMain")
}
