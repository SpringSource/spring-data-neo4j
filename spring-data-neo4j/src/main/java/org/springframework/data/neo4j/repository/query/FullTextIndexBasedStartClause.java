/**
 * Copyright 2013 the original author or authors.
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

import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.repository.query.Parameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a start clause which makes use of a full text index
 * based match against one or more parts.
 *
 * @author Nicki Watt
 */
public class FullTextIndexBasedStartClause extends IndexBasedStartClause {

    public FullTextIndexBasedStartClause(PartInfo partInfo) {
        super(partInfo);
    }

    public FullTextIndexBasedStartClause(List<PartInfo> partInfos) {
        super(partInfos.get(0));
        int i = 0;
        for (PartInfo partInfo: partInfos) {
            if (i != 0) {
                this.merge(partInfo);
            }
            i++;
        }
    }

    @Override
    public String toString() {
        final PartInfo partInfo = getPartInfo();
        final String identifier = partInfo.getIdentifier();
        final String indexName = partInfo.getIndexName();
        final int parameterIndex = partInfo.getParameterIndex();
        return String.format(QueryTemplates.START_CLAUSE_INDEX_QUERY, identifier, indexName, parameterIndex);
    }

    @Override
    public Map<Parameter, Object> resolveParameters(Map<Parameter, Object> parameters, Neo4jTemplate template) {
        Map<Parameter, PartInfo> myParameters = findMyParameters(parameters.keySet());

        Map<Parameter, Object> result = new LinkedHashMap<Parameter, Object>(parameters);
        result.keySet().removeAll(myParameters.keySet());

        final Map<PartInfo, Object> values = matchToPartsAndConvert(myParameters, parameters,template);

        Parameter firstParam = IteratorUtil.first(myParameters.keySet());
        result.put(firstParam, renderQuery(values));
        return result;
    }

    protected String renderQuery(Map<PartInfo, Object> values) {
        StringBuilder sb=new StringBuilder();
        for (Map.Entry<PartInfo, Object> entry : values.entrySet()) {
            if (sb.length()>0) sb.append(" AND ");
            final PartInfo partInfo = entry.getKey();
            Object value = entry.getValue();
            sb.append(QueryTemplates.formatIndexQuery(partInfo,value));
        }
        return sb.toString();
    }




}
