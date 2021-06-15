/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.ProjectionTest1O1;
import org.springframework.data.neo4j.integration.shared.common.ProjectionTestLevel1;
import org.springframework.data.neo4j.integration.shared.common.ProjectionTestRoot;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.ReactiveCypherdslStatementExecutor;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.ReactiveTransactionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.common.NamesOnly;
import org.springframework.data.neo4j.integration.shared.common.NamesOnlyDto;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.integration.shared.common.PersonSummary;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveProjectionIT {

	private static final String FIRST_NAME = "Hans";
	private static final String LAST_NAME = "Mueller";
	private static final String CITY = "Braunschweig";

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;
	private Long projectionTestRootId;
	private Long projectionTest1O1Id;
	private Long projectionTestLevel1Id;

	@Autowired
	ReactiveProjectionIT(Driver driver) {
		this.driver = driver;
	}

	@BeforeEach
	void setup(@Autowired BookmarkCapture bookmarkCapture) {
		Session session = driver.session(bookmarkCapture.createSessionConfig());
		Transaction transaction = session.beginTransaction();

		transaction.run("MATCH (n) detach delete n");

		transaction.run("CREATE (:Person{firstName:'" + FIRST_NAME + "', lastName:'" + LAST_NAME + "'})" + "-[:LIVES_AT]->"
				+ "(:Address{city:'" + CITY + "'})");

		Record result = transaction.run("create (r:ProjectionTestRoot {name: 'root'}) \n"
				+ "create (o:ProjectionTest1O1 {name: '1o1'}) "
				+ "create (l11:ProjectionTestLevel1 {name: 'level11'})\n"
				+ "create (l12:ProjectionTestLevel1 {name: 'level12'})\n"
				+ "create (l21:ProjectionTestLevel2 {name: 'level21'})\n"
				+ "create (l22:ProjectionTestLevel2 {name: 'level22'})\n"
				+ "create (l23:ProjectionTestLevel2 {name: 'level23'})\n"
				+ "create (r) - [:ONE_OONE] -> (o)\n"
				+ "create (r) - [:LEVEL_1] -> (l11)\n"
				+ "create (r) - [:LEVEL_1] -> (l12)\n"
				+ "create (l11) - [:LEVEL_2] -> (l21)\n"
				+ "create (l11) - [:LEVEL_2] -> (l22)\n"
				+ "create (l12) - [:LEVEL_2] -> (l23)\n"
				+ "return id(r), id(l11), id(o)").single();

		projectionTestRootId = result.get(0).asLong();
		projectionTestLevel1Id = result.get(1).asLong();
		projectionTest1O1Id = result.get(2).asLong();
		transaction.commit();
		transaction.close();
		bookmarkCapture.seedWith(session.lastBookmark());
		session.close();
	}

	@Test
	void loadNamesOnlyProjection(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByLastName(LAST_NAME)).assertNext(person -> {
			assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
			assertThat(person.getLastName()).isEqualTo(LAST_NAME);

			String expectedFullName = FIRST_NAME + " " + LAST_NAME;
			assertThat(person.getFullName()).isEqualTo(expectedFullName);
		}).verifyComplete();
	}

	@Test
	void loadPersonSummaryProjection(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByFirstName(FIRST_NAME)).assertNext(person -> {
			assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
			assertThat(person.getLastName()).isEqualTo(LAST_NAME);
			assertThat(person.getAddress()).isNotNull();

			PersonSummary.AddressSummary address = person.getAddress();
			assertThat(address.getCity()).isEqualTo(CITY);
		}).verifyComplete();
	}

	@Test
	void loadNamesOnlyDtoProjection(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByFirstNameAndLastName(FIRST_NAME, LAST_NAME)).assertNext(person -> {
			assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
			assertThat(person.getLastName()).isEqualTo(LAST_NAME);
		}).verifyComplete();
	}

	@Test
	void findDynamicProjectionForNamesOnly(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnly.class))
				.assertNext(person -> {
					assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
					assertThat(person.getLastName()).isEqualTo(LAST_NAME);

					String expectedFullName = FIRST_NAME + " " + LAST_NAME;
					assertThat(person.getFullName()).isEqualTo(expectedFullName);
				}).verifyComplete();
	}

	@Test
	void findDynamicProjectionForPersonSummary(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, PersonSummary.class))
				.assertNext(person -> {
					assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
					assertThat(person.getLastName()).isEqualTo(LAST_NAME);
					assertThat(person.getAddress()).isNotNull();

					PersonSummary.AddressSummary address = person.getAddress();
					assertThat(address.getCity()).isEqualTo(CITY);
				}).verifyComplete();
	}

	@Test
	void findDynamicProjectionForNamesOnlyDto(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnlyDto.class))
				.assertNext(person -> {
					assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
					assertThat(person.getLastName()).isEqualTo(LAST_NAME);
				}).verifyComplete();
	}

	@Test
	void findStringBasedClosedProjection(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.customQueryByFirstName(FIRST_NAME))
				.assertNext(personSummary -> {
					assertThat(personSummary).isNotNull();
					assertThat(personSummary.getFirstName()).isEqualTo(FIRST_NAME);
					assertThat(personSummary.getLastName()).isEqualTo(LAST_NAME);
				})
				.verifyComplete();
	}

	@Test
	void findCypherDSLClosedProjection(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findOne(whoHasFirstName(FIRST_NAME), PersonSummary.class))
				.assertNext(personSummary -> {
					assertThat(personSummary).isNotNull();
					assertThat(personSummary.getFirstName()).isEqualTo(FIRST_NAME);
					assertThat(personSummary.getLastName()).isEqualTo(LAST_NAME);
				})
				.verifyComplete();
	}

	private static Statement whoHasFirstName(String firstName) {
		Node p = Cypher.node("Person").named("p");
		return Cypher.match(p)
				.where(p.property("firstName").isEqualTo(Cypher.anonParameter(firstName)))
				.returning(
						p.getRequiredSymbolicName()
				)
				.build();
	}

	@Test // GH-2164
	void findByIdWithProjectionShouldWork(@Autowired TreestructureRepository repository) {

		StepVerifier.create(repository.findById(projectionTestRootId, SimpleProjection.class))
				.assertNext(projection -> {
					assertThat(projection.getName()).isEqualTo("root");
				})
				.verifyComplete();
	}

	@Test // GH-2165
	void relationshipsShouldBeIncludedInProjections(@Autowired TreestructureRepository repository) {

		StepVerifier.create(repository.findById(projectionTestRootId, SimpleProjectionWithLevelAndLower.class))
				.assertNext(projection ->
						assertThat(projection).satisfies(p -> {
							assertThat(p.getName()).isEqualTo("root");
							assertThat(p.getOneOone()).extracting(ProjectionTest1O1::getName).isEqualTo("1o1");
							assertThat(p.getLevel1()).hasSize(2);
							assertThat(p.getLevel1().stream())
									.anyMatch(e -> e.getId().equals(projectionTestLevel1Id) && e.getLevel2().size() == 2);
				}))
				.verifyComplete();
	}

	@Test // GH-2165
	void nested1to1ProjectionsShouldWork(@Autowired TreestructureRepository repository) {

		StepVerifier.create(repository.findById(projectionTestRootId, ProjectedOneToOne.class))
				.assertNext(projection ->
					assertThat(projection).satisfies(p -> {
						assertThat(p.getName()).isEqualTo("root");
						assertThat(p.getOneOone()).extracting(ProjectedOneToOne.Subprojection::getFullName)
								.isEqualTo(projectionTest1O1Id + " 1o1");
				}))
				.verifyComplete();
	}

	@Test
	void nested1to1ProjectionsWithNestedProjectionShouldWork(@Autowired TreestructureRepository repository) {

		StepVerifier.create(repository.findById(projectionTestRootId, ProjectionWithNestedProjection.class))
				.assertNext(projection ->
						assertThat(projection).satisfies(p -> {
							assertThat(p.getName()).isEqualTo("root");
							assertThat(p.getLevel1()).extracting("name").containsExactlyInAnyOrder("level11", "level12");
							assertThat(p.getLevel1()).flatExtracting("level2").extracting("name")
									.containsExactlyInAnyOrder("level21", "level22", "level23");
						}))
				.verifyComplete();
	}

	@Test // GH-2165
	void nested1toManyProjectionsShouldWork(@Autowired TreestructureRepository repository) {

		StepVerifier.create(repository.findById(projectionTestRootId, ProjectedOneToMany.class))
				.assertNext(projection ->
						assertThat(projection).satisfies(p -> {
							assertThat(p.getName()).isEqualTo("root");
							assertThat(p.getLevel1()).hasSize(2);
						}))
				.verifyComplete();
	}

	@Test // GH-2164
	void findByIdInDerivedFinderMethodInRelatedObjectShouldWork(@Autowired TreestructureRepository repository) {

		StepVerifier.create(repository.findOneByLevel1Id(projectionTestLevel1Id))
				.assertNext(projection -> assertThat(projection.getName()).isEqualTo("root"))
				.verifyComplete();
	}

	@Test // GH-2164
	void findByIdInDerivedFinderMethodInRelatedObjectWithProjectionShouldWork(
			@Autowired TreestructureRepository repository) {

		StepVerifier.create(repository.findOneByLevel1Id(projectionTestLevel1Id, SimpleProjection.class))
				.assertNext(projection -> assertThat(projection.getName()).isEqualTo("root"))
				.verifyComplete();
	}

	interface ReactiveProjectionPersonRepository extends ReactiveNeo4jRepository<Person, Long>,
			ReactiveCypherdslStatementExecutor<Person> {

		Flux<NamesOnly> findByLastName(String lastName);

		Flux<PersonSummary> findByFirstName(String firstName);

		Flux<NamesOnlyDto> findByFirstNameAndLastName(String firstName, String lastName);

		<T> Flux<T> findByLastNameAndFirstName(String lastName, String firstName, Class<T> projectionClass);

		@Query("MATCH (n:Person) where n.firstName = $firstName return n")
		Mono<PersonSummary> customQueryByFirstName(@Param("firstName") String firstName);
	}

	interface TreestructureRepository extends ReactiveNeo4jRepository<ProjectionTestRoot, Long> {

		<T> Mono<T> findById(Long id, Class<T> typeOfProjection);

		Mono<ProjectionTestRoot> findOneByLevel1Id(Long idOfLevel1);

		<T> Mono<T> findOneByLevel1Id(Long idOfLevel1, Class<T> typeOfProjection);
	}

	interface SimpleProjection {

		String getName();
	}

	interface SimpleProjectionWithLevelAndLower {

		String getName();

		ProjectionTest1O1 getOneOone();

		List<ProjectionTestLevel1> getLevel1();
	}

	interface ProjectedOneToOne {

		String getName();

		Subprojection getOneOone();

		interface Subprojection {

			/**
			 * @return Some arbitrary computed projection result to make sure that machinery works as well
			 */
			@Value("#{target.id + ' ' + target.name}")
			String getFullName();
		}
	}

	interface ProjectedOneToMany {

		String getName();

		List<Subprojection> getLevel1();

		interface Subprojection {

			/**
			 * @return Some arbitrary computed projection result to make sure that machinery works as well
			 */
			@Value("#{target.id + ' ' + target.name}")
			String getFullName();
		}
	}

	interface ProjectionWithNestedProjection {

		String getName();

		List<Subprojection1> getLevel1();

		interface Subprojection1 {
			String getName();
			List<Subprojection2> getLevel2();
		}

		interface Subprojection2 {
			String getName();
		}
	}

	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}

	}

}
