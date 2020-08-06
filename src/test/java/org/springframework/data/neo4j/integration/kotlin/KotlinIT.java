/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.kotlin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.KotlinPerson;
import org.springframework.data.neo4j.integration.shared.KotlinRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class KotlinIT {

	private final static String PERSON_NAME = "test";

	private static Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	@Autowired
	KotlinIT(Driver driver) {
		this.driver = driver;
	}

	@BeforeEach
	void setup() {
		try (Session session = driver.session(); Transaction transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n").consume();
			transaction.run("CREATE (n:KotlinPerson) SET n.name = $personName", Values.parameters("personName", PERSON_NAME))
					.consume();
			transaction.commit();
		}
	}

	@Test
	void findAllKotlinPersons(@Autowired KotlinRepository repository) {

		Iterable<KotlinPerson> person = repository.findAll();
		assertThat(person.iterator().next().getName()).isEqualTo(PERSON_NAME);
	}

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = KotlinPerson.class)
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

	}
}
