/**
 * Copyright 2011-2015 the original author or authors.
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

package org.springframework.data.neo4j.repository;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.repository.query.GraphQueryMethod;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.*;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @author mh
 * @author Oliver Gierke
 * @since 28.03.11
 */
public class GraphRepositoryFactory extends RepositoryFactorySupport {

    private final Neo4jTemplate template;
    private final Neo4jMappingContext mappingContext;

    /**
     * Creates a new {@link GraphRepositoryFactory} from the given {@link org.springframework.data.neo4j.support.Neo4jTemplate} and
     * {@link MappingContext}.
     *
     * @param template must not be {@literal null}.
     * @param mappingContext must not be {@literal null}.
     */
    public GraphRepositoryFactory(Neo4jTemplate template, Neo4jMappingContext mappingContext) {

        Assert.notNull(template);
        Assert.notNull(mappingContext);

        this.template = template;
        this.mappingContext = mappingContext;
    }


    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryInformation)
     */
    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        return getTargetRepository(information, template);
    }

    protected Object getTargetRepository(RepositoryInformation metadata, Neo4jTemplate template) {
        return getTargetRepositoryViaReflection(metadata, metadata.getDomainType(), template);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        Class<?> domainClass = repositoryMetadata.getDomainType();

        @SuppressWarnings("rawtypes")
        final GraphEntityInformation entityInformation = (GraphEntityInformation) getEntityInformation(domainClass);
        if (entityInformation.isNodeEntity()) {
            return NodeGraphRepositoryImpl.class;
        }
        if (entityInformation.isRelationshipEntity()) {
            return RelationshipGraphRepository.class;
        }
        throw new IllegalArgumentException("Invalid Domain Class "+ domainClass+" neither Node- nor RelationshipEntity");
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> type) {
        return new GraphMetamodelEntityInformation(type, template);
    }


    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key, EvaluationContextProvider evaluationContextProvider) {
        return new QueryLookupStrategy() {
            @Override
            public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
                final GraphQueryMethod queryMethod = new GraphQueryMethod(method, metadata, factory, namedQueries, mappingContext);
                return queryMethod.createQuery(GraphRepositoryFactory.this.template);
            }
        };
    }


}
