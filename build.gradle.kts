plugins {
    `java-gradle-plugin`
}

group = "name.jurgenei.gradle"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

gradlePlugin {
    plugins {
        create("xmlTransformPlugin") {
            id = "name.jurgenei.gradle.xml"
            implementationClass = "name.jurgenei.gradle.xml.XmlTransformPlugin"
            displayName = "Saxon XML Transform Plugin"
            description = "Adds XSLT and XQuery tasks backed by Saxon"
        }
    }
}

dependencies {
    implementation("net.sf.saxon:Saxon-HE:12.5")

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

