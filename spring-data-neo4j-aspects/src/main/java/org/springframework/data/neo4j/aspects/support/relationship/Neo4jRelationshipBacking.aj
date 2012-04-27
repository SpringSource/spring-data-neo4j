/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.neo4j.aspects.support.relationship;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.aspects.core.RelationshipBacked;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityStateFactory;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.lang.reflect.Field;

import static org.springframework.data.neo4j.support.DoReturn.unwrap;

/**
 * Aspect for handling relationship entity creation and field access (read & write)
 * puts the underlying state into and delegates field access to an {@link org.springframework.data.neo4j.core.EntityState} instance,
 * created by a configured {@link RelationshipEntityStateFactory}
 */
public aspect Neo4jRelationshipBacking {
	
    protected final Log log = LogFactory.getLog(getClass());

    declare parents : (@RelationshipEntity *) implements RelationshipBacked;

    /**
     * pointcut for constructors not taking a node to be handled by the aspect and the {@link org.springframework.data.neo4j.core.EntityState}
     */
	pointcut arbitraryUserConstructorOfRelationshipBackedObject(RelationshipBacked entity) :
		execution((@RelationshipEntity *).new(..)) &&
		!execution((@RelationshipEntity *).new(Relationship)) &&
		this(entity) && !cflowbelow(call(* fromStateInternal(..)));


    /**
     * Handle outside entity instantiation by either creating an appropriate backing node in the graph or in the case
     * of a reinstantiated partial entity by assigning the original node to the entity, the concrete behaviour is delegated
     * to the {@link org.springframework.data.neo4j.core.EntityState}. Also handles the java type representation in the graph.
     * When running outside of a transaction, no node is created, this is handled later when the entity is accessed within
     * a transaction again.
     */
    before(RelationshipBacked entity): arbitraryUserConstructorOfRelationshipBackedObject(entity) {
        if (entityStateFactory == null) {
            log.error("entityStateFactory not set, not creating accessors for " + entity.getClass());
        } else {
            if (entity.entityState != null) return;
            entity.entityState = entityStateFactory.getEntityState(entity, true);
        }
    }


    protected pointcut entityFieldGet(RelationshipBacked entity) :
            get(!transient * RelationshipBacked+.*) &&
            this(entity) &&
            !get(* RelationshipBacked.*);


    protected pointcut entityFieldSet(RelationshipBacked entity, Object newVal) :
            set(!transient * RelationshipBacked+.*) &&
            this(entity) &&
            args(newVal) &&
            !set(* RelationshipBacked.*);

	private Neo4jTemplate template;
    private RelationshipEntityStateFactory entityStateFactory;


    public void setTemplate(Neo4jTemplate template) {
        this.template = template;
    }

    public void setRelationshipEntityStateFactory(RelationshipEntityStateFactory entityStateFactory) {
        this.entityStateFactory = entityStateFactory;
    }

    /**
     * field for {@link org.springframework.data.neo4j.core.EntityState} that takes care of all entity operations
     */
    private EntityState<Relationship> RelationshipBacked.entityState;


    public Neo4jTemplate RelationshipBacked.getTemplate() {
        return Neo4jRelationshipBacking.aspectOf().template;
    }

	public void RelationshipBacked.setPersistentState(Relationship r) {
        if (this.entityState == null) {
            this.entityState = Neo4jRelationshipBacking.aspectOf().entityStateFactory.getEntityState(this, true);
        }
        this.entityState.setPersistentState(r);
	}
	
	public Relationship RelationshipBacked.getPersistentState() {
		return this.entityState!=null ? this.entityState.getPersistentState() : null;
	}

	public boolean RelationshipBacked.hasPersistentState() {
		return this.entityState!=null && this.entityState.hasPersistentState();
	}

	public Long RelationshipBacked.getRelationshipId() {
        if (!hasPersistentState()) return null;
		return getPersistentState().getId();
	}


    public EntityState<Relationship> RelationshipBacked.getEntityState() {
        return this.entityState;
    }


    /**
     * @param obj
     * @return result of equality check of the underlying relationship
     */
	public final boolean RelationshipBacked.equals(Object obj) {
		if (this==obj) return true;
        if (!hasPersistentState()) return false;
        if (obj instanceof RelationshipBacked) {
			return this.getPersistentState().equals(((RelationshipBacked) obj).getPersistentState());
		}
		return false;
	}

    /**
     * @return hashCode of the underlying relationship
     */
	public final int RelationshipBacked.hashCode() {
        if (!hasPersistentState()) return System.identityHashCode(this);
		return getPersistentState().hashCode();
	}

    public <T extends RelationshipBacked> T RelationshipBacked.persist() {
        return (T)this.entityState.persist();
    }

	public void RelationshipBacked.remove() {
	     Neo4jRelationshipBacking.aspectOf().template.delete(this);
	}

    public <R extends RelationshipBacked> R  RelationshipBacked.projectTo(Class<R> targetType) {
        return (R)Neo4jRelationshipBacking.aspectOf().template.projectTo(this, targetType);
    }

    Object around(RelationshipBacked entity): entityFieldGet(entity) {
        if (entity.entityState == null) return proceed(entity);
        Object result = entity.entityState.getValue(field(thisJoinPoint),null);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity);
    }

    Object around(RelationshipBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
        if (entity.entityState == null) return proceed(entity,newVal);
        Object result=entity.entityState.setValue(field(thisJoinPoint),newVal,null);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity,result);
	}


    Field field(JoinPoint joinPoint) {
        FieldSignature fieldSignature = (FieldSignature)joinPoint.getSignature();
        return fieldSignature.getField();
    }
}
