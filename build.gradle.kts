plugins {
    `java-gradle-plugin`
    jacoco
    `maven-publish`
    signing
    id("com.gradle.plugin-publish") version "2.1.1"
    id("org.owasp.dependencycheck") version "10.0.3"
    id("com.github.spotbugs") version "6.1.0"
    id("org.sonarqube") version "6.0.1.5171"
}

group = "name.jurgenei.gradle"
version = "0.1.2"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

gradlePlugin {
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

publishing {
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

signing {
    useGpgCmd()
    sign(publishing.publications)
}

// OWASP Dependency-Check configuration
dependencyCheck {
    format = "HTML,JSON,XML"
    failBuildOnCVSS = 7.0f
    suppressionFile = "dependency-check-suppressions.xml"

    // NVD API key configuration (improves scan speed by 30-50%)
    // Get key from: https://nvd.nist.gov/developers/request-an-api-key
    nvd.apiKey = providers.gradleProperty("org.owasp.dependencycheck.nvd.api.key").orNull
        ?: System.getenv("NVD_API_KEY")
}

// SpotBugs configuration
configure<com.github.spotbugs.snom.SpotBugsExtension> {
    ignoreFailures.set(false)
    effort.set(com.github.spotbugs.snom.Effort.DEFAULT)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
}

tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsMain") {
    reports.maybeCreate("html").apply {
        required.set(true)
    }
    reports.maybeCreate("xml").apply {
        required.set(false)
    }
}

// SonarQube configuration
sonar {
    properties {
        property("sonar.projectKey", "gradle-xml-plugin")
        property("sonar.projectName", "Gradle XML Plugin")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.java.source", "21")
    }
}

dependencies {
    implementation("net.sf.saxon:Saxon-HE:12.5")
    implementation("name.dmaus.schxslt:schxslt2:1.10.3")

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
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
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.register("allSecurityChecks") {
    group = "verification"
    description = "Run all security and quality checks (Dependency-Check, SpotBugs, SonarQube)"
    dependsOn("check", "dependencyCheck", "spotbugsMain")
}
