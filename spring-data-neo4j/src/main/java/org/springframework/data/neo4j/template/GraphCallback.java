/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.template;

import org.springframework.data.neo4j.core.GraphDatabase;

public interface GraphCallback<T> {
    T doWithGraph(final GraphDatabase graph) throws Exception;

    public abstract class WithoutResult implements GraphCallback<Void> {
        @Override
        public Void doWithGraph(GraphDatabase graph) throws Exception {
            doWithGraphWithoutResult(graph);
            return null;
        }
        public abstract void doWithGraphWithoutResult(GraphDatabase graph) throws Exception;
    }
}
