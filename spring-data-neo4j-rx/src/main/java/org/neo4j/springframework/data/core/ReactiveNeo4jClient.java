/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.reactive.RxStatementRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.springframework.data.core.Neo4jClient.BindSpec;
import org.neo4j.springframework.data.core.Neo4jClient.MappingSpec;
import org.neo4j.springframework.data.core.Neo4jClient.RecordFetchSpec;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

/**
 * Reactive Neo4j client. The main difference to the {@link Neo4jClient imperative Neo4j client} is the fact that all
 * operations will only be executed once something subscribes to the reactive sequence defined.
 *
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface ReactiveNeo4jClient {

	LogAccessor cypherLog = new LogAccessor(LogFactory.getLog("org.neo4j.springframework.data.cypher"));

	static ReactiveNeo4jClient create(Driver driver) {

		return new DefaultReactiveNeo4jClient(driver);
	}

	/**
	 * Entrypoint for creating a new Cypher query. Doesn't matter at this point whether it's a match, merge, create or
	 * removal of things.
	 *
	 * @param cypher The cypher code that shall be executed
	 * @return A new CypherSpec
	 */
	ReactiveRunnableSpec query(String cypher);

	/**
	 * Entrypoint for creating a new Cypher query based on a supplier. Doesn't matter at this point whether it's a match,
	 * merge, create or removal of things. The supplier can be an arbitrary Supplier that may provide a DSL for generating
	 * the Cypher statement.
	 *
	 * @param cypherSupplier A supplier of arbitrary Cypher code
	 * @return A runnable query specification.
	 */
	ReactiveRunnableSpec query(Supplier<String> cypherSupplier);

	/**
	 * Delegates interaction with the default database to the given callback.
	 *
	 * @param callback A function receiving a reactive statement runner for database interaction that can optionally return a publisher with none or exactly one element
	 * @param <T>      The type of the result being produced
	 * @return A single publisher containing none or exactly one element that will be produced by the callback
	 */
	<T> OngoingReactiveDelegation<T> delegateTo(Function<RxStatementRunner, Mono<T>> callback);

	/**
	 * Takes a prepared query, containing all the information about the cypher template to be used, needed parameters and
	 * an optional mapping function, and turns it into an executable query.
	 *
	 * @param preparedQuery prepared query that should get converted to an executable query
	 * @param <T>           The type of the objects returned by this query.
	 * @return              An executable query
	 */
	<T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery);

	/**
	 * Contract for a runnable query that can be either run returning it's result, run without results or be parameterized.
	 * @since 1.0
	 */
	interface ReactiveRunnableSpec extends ReactiveRunnableSpecTightToDatabase {

		/**
		 * Pins the previously defined query to a specific database.
		 *
		 * @param targetDatabase selected database to use
		 * @return A runnable query specification that is now tight to a given database.
		 */
		ReactiveRunnableSpecTightToDatabase in(String targetDatabase);
	}

	/**
	 * Contract for a runnable query inside a dedicated database.
	 * @since 1.0
	 */
	interface ReactiveRunnableSpecTightToDatabase extends BindSpec<ReactiveRunnableSpecTightToDatabase> {

		/**
		 * Create a mapping for each record return to a specific type.
		 *
		 * @param targetClass The class each record should be mapped to
		 * @param <T>         The type of the class
		 * @return A mapping spec that allows specifying a mapping function
		 */
		<T> MappingSpec<Mono<T>, Flux<T>, T> fetchAs(Class<T> targetClass);

		/**
		 * Fetch all records mapped into generic maps
		 *
		 * @return A fetch specification that maps into generic maps
		 */
		RecordFetchSpec<Mono<Map<String, Object>>, Flux<Map<String, Object>>, Map<String, Object>> fetch();

		/**
		 * Execute the query and discard the results. It returns the drivers result summary, including various counters
		 * and other statistics.
		 *
		 * @return A mono containing the native summary of the query.
		 */
		Mono<ResultSummary> run();
	}

	/**
	 * A contract for an ongoing delegation in the selected database.
	 *
	 * @param <T> The type of the returned value.
	 * @since 1.0
	 */
	interface OngoingReactiveDelegation<T> extends ReactiveRunnableDelegation<T> {

		/**
		 * Runs the delegation in the given target database.
		 *
		 * @param targetDatabase selected database to use
		 * @return An ongoing delegation
		 */
		ReactiveRunnableDelegation<T> in(String targetDatabase);
	}

	/**
	 * A runnable delegation.
	 *
	 * @param <T> the type that gets returned by the query
	 * @since 1.0
	 */
	interface ReactiveRunnableDelegation<T> {

		/**
		 * Runs the stored callback.
		 *
		 * @return The optional result of the callback that has been executed with the given database.
		 */
		Mono<T> run();
	}

	/**
	 * An interface for controlling query execution in a reactive fashion.
	 *
	 * @param <T> the type that gets returned by the query
	 * @since 1.0
	 */
	interface ExecutableQuery<T> {

		/**
		 * @return All results returned by this query.
		 */
		Flux<T> getResults();

		/**
		 * @return A single result
		 * @throws IncorrectResultSizeDataAccessException if there are more than one result
		 */
		Mono<T> getSingleResult();
	}
}