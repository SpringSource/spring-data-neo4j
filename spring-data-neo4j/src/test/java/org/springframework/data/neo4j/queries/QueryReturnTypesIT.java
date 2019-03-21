/*
 * Copyright 2011-2019 the original author or authors.
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

package org.springframework.data.neo4j.queries;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.galaxy.context.GalaxyContext;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.examples.galaxy.repo.WorldRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
@ContextConfiguration(classes = { GalaxyContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class QueryReturnTypesIT extends MultiDriverTestClass {

	@Autowired WorldRepository worldRepository;

	@Autowired Session session;

	@Test
	public void shouldCallExecuteWhenPrimitiveVoidReturnTypeOnQuery() {

		World world = new World("Tatooine", 0);

		worldRepository.save(world);
		worldRepository.touchAllWorlds();

		session.clear();
		world = worldRepository.findOne(world.getId());
		assertNotNull(world.getUpdated());
	}

	/**
	 * @see DATAGRAPH-704
	 */
	@Test
	public void shouldCallExecuteWhenVoidReturnTypeOnQuery() {

		World tatooine = new World("Tatooine", 0);
		World dagobah = new World("Dagobah", 0);

		tatooine.addRocketRouteTo(dagobah);

		worldRepository.save(tatooine);
		worldRepository.touchAllWorlds();

		session.clear();
		tatooine = worldRepository.findOne(tatooine.getId());

		assertNotNull(tatooine.getUpdated());
		assertEquals(1, tatooine.getReachableByRocket().size());

		for (World world : tatooine.getReachableByRocket()) {
			assertNotNull(world.getUpdated());
		}
	}

	@Test
	public void shouldReturnStatisticsIfRequested() {

		World tatooine = new World("Tatooine", 0);

		worldRepository.save(tatooine);

		QueryStatistics stats = worldRepository.touchAllWorldsWithStatistics().queryStatistics();

		assertEquals(1, stats.getPropertiesSet());
	}
}
