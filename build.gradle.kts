plugins {
  kotlin("jvm") version "1.7.10"
  kotlin("plugin.allopen") version "1.7.10"
  id("io.quarkus")
}

repositories {
  mavenCentral()
  mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
  implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
  implementation("io.quarkus:quarkus-arc")
  implementation("io.quarkus:quarkus-keycloak-admin-client")
  implementation("io.quarkus:quarkus-keycloak-authorization")
  implementation("io.quarkus:quarkus-kotlin")
  implementation("io.quarkus:quarkus-oidc")
  implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  testImplementation("io.quarkus:quarkus-junit5")
  testImplementation("io.quarkus:quarkus-test-keycloak-server")
  testImplementation("io.rest-assured:kotlin-extensions")
  testImplementation("io.rest-assured:rest-assured")
}

group = "org.acme"
version = "1.0.0-SNAPSHOT"

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

allOpen {
  annotation("javax.ws.rs.Path")
  annotation("javax.enterprise.context.ApplicationScoped")
  annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
  kotlinOptions.javaParameters = true
}

tasks.test {
  // enable logging config during tests
  systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
  testLogging.showExceptions = true
  testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
}
