/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.repository;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.domain.sample.NodeWithUUIDAsId;
import org.springframework.data.neo4j.domain.sample.SampleEntity;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = Neo4jRepositoryTests.Config.class)
@Transactional
public class Neo4jRepositoryTests {

	@Autowired SampleEntityRepository repository;

	@Autowired NodeWithUUIDAsIdRepository nodeWithUUIDAsIdRepository;

	@Test
	public void testCrudOperationsForCompoundKeyEntity() throws Exception {

		SampleEntity entity = new SampleEntity("foo", "bar");
		repository.save(entity);
		assertThat(repository.existsById(entity.getId()), is(true));
		assertThat(repository.count(), is(1L));

		Optional<SampleEntity> optional = repository.findById(entity.getId());
		assertTrue(optional.isPresent());
		optional.ifPresent(actual -> assertThat(actual, is(entity)));

		repository.deleteAll(Arrays.asList(entity));
		assertThat(repository.count(), is(0L));
	}

	@Test // DATAGRAPH-1144
	public void explicitIdsWithCustomTypesShouldWork() throws Exception {

		NodeWithUUIDAsId entity = new NodeWithUUIDAsId("someProperty");
		nodeWithUUIDAsIdRepository.save(entity);

		assertThat(nodeWithUUIDAsIdRepository.existsById(entity.getMyNiceId()), is(true));
		assertThat(nodeWithUUIDAsIdRepository.count(), is(1L));

		Optional<NodeWithUUIDAsId> retrievedEntity = nodeWithUUIDAsIdRepository.findById(entity.getMyNiceId());
		assertTrue(retrievedEntity.isPresent());
		assertThat(retrievedEntity.get(), is(entity));

		nodeWithUUIDAsIdRepository.deleteAll(Arrays.asList(entity));
		assertThat(nodeWithUUIDAsIdRepository.count(), is(0L));
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.domain.sample")
	static class Config {}
}

interface SampleEntityRepository extends Neo4jRepository<SampleEntity, Long> {}

interface NodeWithUUIDAsIdRepository extends Neo4jRepository<NodeWithUUIDAsId, UUID> {}
