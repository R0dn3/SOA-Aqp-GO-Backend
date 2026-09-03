plugins {
	java
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.integrador"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")

	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-web-services")
	implementation("wsdl4j:wsdl4j")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")

	//actuator
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("io.sentry:sentry-spring-boot-4-starter:8.43.2")

	//Jwt
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	//firebase
	implementation("com.google.firebase:firebase-admin:9.3.0")

	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")

	// Solución definitiva para SOAP/JAXB en Spring Boot 4 + Java 21
	implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
	runtimeOnly("com.sun.xml.bind:jaxb-impl:4.0.5")
	implementation("org.glassfish.jaxb:jaxb-runtime:4.0.5")
	implementation("com.sun.xml.messaging.saaj:saaj-impl:3.0.4")

}

// Resuelve la duplicación física de esquemas XSD compilados en las librerías del empaquetado BOOT-INF
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Test> {
	useJUnitPlatform()
}
