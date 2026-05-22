plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "name.jurgenei.gradle"
version = "0.1.0"

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

dependencies {
    implementation("net.sf.saxon:Saxon-HE:12.5")
    implementation("name.dmaus.schxslt:schxslt2:1.10.3")

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

