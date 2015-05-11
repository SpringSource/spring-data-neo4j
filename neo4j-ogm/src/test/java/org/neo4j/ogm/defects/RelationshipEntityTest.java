/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.defects;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.ogm.domain.cineasts.annotated.Actor;
import org.neo4j.ogm.domain.cineasts.annotated.Movie;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;

/**
 * @author Luanne Misquitta
 */
public class RelationshipEntityTest {

	@Rule
	public Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule();

	private Session session;

	@Before
	public void init() throws IOException {
		session = new SessionFactory("org.neo4j.ogm.domain.cineasts.annotated").openSession(neo4jRule.baseNeoUrl());
	}

	/**
	 * @see DATAGRAPH-615
	 */
	@Test
	public void testThatRelationshipEntityIsLoadedWhenWhenTypeIsNotDefined() {
		Movie hp = new Movie();
		hp.setTitle("Goblet of Fire");
		hp.setYear(2005);

		Actor daniel = new Actor("Daniel Radcliffe");
		daniel.nominatedFor(hp, "Saturn Award", 2005);

		session.save(daniel);

		session.clear();

		daniel = session.load(Actor.class,daniel.getId());
		assertNotNull(daniel);
		assertEquals(1, daniel.getNominations().size()); //fails
	}

}
