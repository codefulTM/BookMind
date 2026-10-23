plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.bookmind"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

// Spring Boot BOM quản lý flyway-core; module SQLite phải cùng version với core.
val flywayVersion = "12.4.0"

dependencies {
	// Spring Boot
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	// WebClient/Reactor cho client AI streaming; app vẫn chạy servlet (Web MVC).
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-flyway")

	// Persistence: SQLite + Hibernate dialect + Flyway SQLite support
	implementation("org.xerial:sqlite-jdbc")
	implementation("org.hibernate.orm:hibernate-community-dialects")
	implementation("org.flywaydb:flyway-database-nc-sqlite:$flywayVersion")

	// Trích xuất nội dung + đếm token
	implementation("org.apache.pdfbox:pdfbox:3.0.8")
	implementation("org.jsoup:jsoup:1.23.2")
	implementation("com.knuddels:jtokkit:1.1.0")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.wiremock:wiremock-standalone:3.13.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	// Không ghi DB test vào ~/.bookmind của máy dev.
	systemProperty("bookmind.home", layout.buildDirectory.dir("test-home").get().asFile.absolutePath)
}
