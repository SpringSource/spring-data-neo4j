/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.annotation;

import org.springframework.data.annotation.Persistent;

import java.lang.annotation.*;

/**
 * Annotation to declare an Pojo-Entity as graph backed.
 * 
 * @author Michael Hunger
 * @since 27.08.2010
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Persistent
public @interface NodeEntity {
    /**
     * @return true if the property names default to field names, otherwise the FQN of the class will be prepended
     */
    boolean useShortNames() default true;

    /**
     * <p>
     * If partial is set, then construction of the node is delayed until the entity's id has been set by another persistent store. Only
     * {@link org.springframework.data.neo4j.annotation.GraphProperty} annotated fields will be handled by the graph storage.
     * </p>
     *
     * <p>
     * Currently, only JPA storage is supported for partial node entities.
     * </p>
     *
     * @return true if the entity is only partially managed by the Neo4jNodeBacking aspect.
     */
    boolean partial() default false;


}
