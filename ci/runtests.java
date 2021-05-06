///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//REPOS mavencentral,spring-libs-snapshot=https://repo.spring.io/libs-snapshot,acme=file:///Users/msimons/Desktop/r

// Hard requirements for this script, not derivable from Maven
//FILES logback-silent.xml
//DEPS org.junit.platform:junit-platform-launcher:1.7.1
//DEPS org.springframework.data:spring-data-neo4j:6.1.1-GH-2245-SNAPSHOT
//DEPS org.springframework.data:spring-data-neo4j:6.1.1-GH-2245-SNAPSHOT:tests@test-jar

// Generated dependencies (Not done yet…)
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.3
//DEPS com.querydsl:querydsl-core:4.4.0
//DEPS eu.michael-simons.neo4j:junit-jupiter-causal-cluster-testcontainer-extension:2020.0.7
//DEPS io.mockk:mockk:1.11.0
//DEPS io.projectreactor:reactor-core:3.4.6-SNAPSHOT
//DEPS io.projectreactor:reactor-test:3.4.6-SNAPSHOT
//DEPS io.r2dbc:r2dbc-h2:0.8.4.RELEASE
//DEPS io.reactivex:rxjava:1.3.8
//DEPS io.reactivex:rxjava-reactive-streams:1.2.1
//DEPS io.reactivex.rxjava2:rxjava:2.2.5
//DEPS javax.enterprise:cdi-api:2.0.SP1
//DEPS javax.transaction:jta:1.1
//DEPS org.apiguardian:apiguardian-api:1.1.0
//DEPS org.jboss.weld.se:weld-se-core:3.1.4.Final
//DEPS org.jetbrains.kotlin:kotlin-reflect:1.5.0
//DEPS org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.0
//DEPS org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3
//DEPS org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.4.3
//DEPS org.neo4j:neo4j-cypher-dsl:2021.2.0
//DEPS org.neo4j.driver:neo4j-java-driver:4.2.4
//DEPS org.springframework:spring-beans:5.3.7-SNAPSHOT
//DEPS org.springframework:spring-context:5.3.7-SNAPSHOT
//DEPS org.springframework:spring-core:5.3.7-SNAPSHOT
//DEPS org.springframework:spring-tx:5.3.7-SNAPSHOT
//DEPS org.springframework.data:spring-data-commons:2.5.1-SNAPSHOT
//DEPS org.springframework.data:spring-data-r2dbc:1.1.0.RELEASE
//DEPS org.testcontainers:junit-jupiter:1.15.2
//DEPS org.testcontainers:neo4j:1.15.2
//DEPS org.junit.jupiter:junit-jupiter:5.7.1
//DEPS org.junit.vintage:junit-vintage-engine:5.7.1
//DEPS org.mockito:mockito-core:3.7.7
//DEPS org.mockito:mockito-junit-jupiter:3.7.7
//DEPS org.assertj:assertj-core:3.19.0
//DEPS org.springframework:spring-test:5.3.7-SNAPSHOT
//DEPS org.slf4j:slf4j-api:1.7.26
//DEPS ch.qos.logback:logback-classic:1.2.3
//DEPS org.projectlombok:lombok:1.18.20

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.launcher.TagFilter.excludeTags;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.support.RetryExceptionPredicate;

public class runtests {

	public static void main(String... args) throws IOException {

		var logConfig = Files.createTempFile("logback", ".xml");
		try (var s = runtests.class.getResourceAsStream("logback-silent.xml")) {
			Files.copy(s, logConfig, StandardCopyOption.REPLACE_EXISTING);
		}
		System.setProperty("logback.configurationFile", logConfig.toAbsolutePath().normalize().toString());

		var log = LoggerFactory.getLogger(runtests.class);

		var listener = new SummaryGeneratingListener();
		var launcher = LauncherFactory
				.create(LauncherConfig.builder()
						.addTestExecutionListeners(new TestExecutionListener() {
							@Override public void executionStarted(TestIdentifier testIdentifier) {
								if (testIdentifier.isContainer() && testIdentifier.getParentId().isPresent()) {
									log.debug(testIdentifier.getUniqueId());
								}
							}
						})
						.addTestExecutionListeners(listener).build());

		var canRetry = new RetryExceptionPredicate();

		var selectors = List.<DiscoverySelector>of(selectPackage("org.springframework.data.neo4j.integration"));
		var maxRetries = 100;
		var counter = 0;

		while (!selectors.isEmpty() && ++counter <= maxRetries) {
			log.info("Attempt {}/{}", counter, maxRetries);
			var request = LauncherDiscoveryRequestBuilder.request()
					.selectors(selectors)
					.filters(
							includeClassNamePatterns(".*IT.*"),
							excludeTags("incompatible-with-aura")
					)
					.build();

			launcher.execute(request);

			var summary = listener.getSummary();
			var failures = summary.getFailures();

			if (failures.isEmpty()) {
				System.exit(0);
			}

			var failuresByState = failures.stream().collect(partitioningBy(failure -> canRetry.test(failure.getException())));
			if (!failuresByState.get(false).isEmpty()) {
				log.error("The following tests failed in non retryable ways:");
				failuresByState.get(false)
						.forEach(failure -> failure.getException().printStackTrace());
				System.exit(1);
			}

			selectors = failuresByState.get(true).stream()
					.map(failure -> DiscoverySelectors.selectUniqueId(failure.getTestIdentifier().getUniqueId()))
					.collect(toList());
		}
		log.error("Several tests failed despite a number of retries.");
		System.exit(1);
	}
}
