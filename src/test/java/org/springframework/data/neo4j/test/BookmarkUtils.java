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
package org.springframework.data.neo4j.test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import org.neo4j.driver.Bookmark;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Don't do this without good reasons.
 * It is in our test code to so that we can provide fixtures through regular driver sessions without applying them
 * via SDN itself. If we do so, we must "forward" the transaction managers when we test against clusters so that everything
 * works well under from the same last write.
 *
 * @author Michael J. Simons
 */
public final class BookmarkUtils {

	public static void fastForwardTo(TransactionTemplate transactionTemplate, Bookmark lastBookmark) {

		TransactionSynchronizationManager.bindResource(Neo4jTransactionManager.RESOURCE_KEY_LAST_BOOKMARKS, Collections.singleton(lastBookmark));
		transactionTemplate.executeWithoutResult(tx -> {
		});
	}

	public static void fastForwardTo(TransactionalOperator transactionalOperator, Bookmark lastBookmark) {

		transactionalOperator.transactional(Mono.just(""))
				.contextWrite(ctx -> ctx.put(ReactiveNeo4jTransactionManager.CONTEXT_KEY_LAST_BOOKMARKS,
						Collections.singleton(lastBookmark)))
				.as(StepVerifier::create).expectNextCount(1L)
				.verifyComplete();
	}

	private BookmarkUtils() {
	}
}
