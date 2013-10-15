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
package org.springframework.data.neo4j.repository.query;

import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.data.neo4j.repository.query.QueryTemplates.INDEXBASED_WHERE_TYPE_CHECK;
import static org.springframework.data.neo4j.repository.query.QueryTemplates.LABELBASED_WHERE_TYPE_CHECK;
import static org.springframework.util.StringUtils.*;

/**
 * Representation of a Cypher {@literal where} clause specifically for
 * use to narrow the results based on particular entity types, where
 * those entities can be identified via specific Labels (as per the
 * Label Based Type Representation Strategy)
 *
 * @author Nicki Watt
 */
public class LabelBasedTypeRestrictingWhereClause extends WhereClause {
    private String aliases;

    public LabelBasedTypeRestrictingWhereClause(PartInfo partInfo, Neo4jPersistentEntity entity, Neo4jTemplate template) {
        super(partInfo, template);
        Object[] aliasAsArray = collectAliases(entity.getEntityType()).toArray();
        aliases = arrayToDelimitedString(aliasAsArray, ":");
    }

    @Override
    public String toString() {
        return String.format(LABELBASED_WHERE_TYPE_CHECK, partInfo.getIdentifier(), aliases);
    }

    private Set<String> collectAliases(StoredEntityType entityType) {
        Set<String> aliases = new HashSet<String>();
        aliases.add("`"+entityType.getAlias().toString()+"`");
        for (StoredEntityType superType : entityType.getSuperTypes()) {
            aliases.addAll(collectAliases(superType));
        }
        return aliases;
    }
}
