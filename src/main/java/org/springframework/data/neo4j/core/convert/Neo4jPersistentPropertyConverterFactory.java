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
package org.springframework.data.neo4j.core.convert;

import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;

/**
 * This interface needs to be implemented to provide custom configuration for a {@link Neo4jPersistentPropertyConverter}. Use cases may
 * be specific date formats or the like. The build method will receive the whole property. It is safe to assume that at
 * least  the {@link ConvertWith @ConvertWith} annotation is present on the property, either directly or meta-annotated.
 * <p>Classes implementing this interface should have a default constructor. In a normal Spring setup (not CDI), they
 * might declare autowired constructor parameters, too.
 *
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Abwasser
 * @since 6.0
 */
public interface Neo4jPersistentPropertyConverterFactory {

	/**
	 * @param persistentProperty The property for which the converter should be build.
	 * @return The new or existing converter
	 */
	Neo4jPersistentPropertyConverter getPropertyConverterFor(Neo4jPersistentProperty persistentProperty);
}
