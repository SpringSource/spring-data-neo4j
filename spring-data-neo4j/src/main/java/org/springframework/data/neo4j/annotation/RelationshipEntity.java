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
 * Annotation to declare a POJO-entity as graph-backed relationship entity.
 * It is used by the Neo4jRelationshipBacking aspect to add field advices as well as the RelationshipBacked interface.
 *
 * Relationship entities cannot be instantiated directly. They will be provided by relationship fields and the
 * methods relatedTo and getRelationshipTo introduced to the node entities.
 *
 * @author Michael Hunger
 * @since 27.08.2010
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Persistent
public @interface RelationshipEntity {
    /**
     * @return true if the property names default to field names, otherwise the FQN of the class will be prepended
     */
    boolean useShortNames() default true;

    /**
     * @return the relationship-type of this entity will be used if no other type is provided, either as field inside
     * the relationship-entity or via an annotation in the node-entity, this type will take precedence over node-entity #
     * field names.
     */
    String type() default "";
}
