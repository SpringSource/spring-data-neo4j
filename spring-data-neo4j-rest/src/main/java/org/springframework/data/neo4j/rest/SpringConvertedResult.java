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
package org.springframework.data.neo4j.rest;

import org.neo4j.rest.graphdb.util.ConvertedResult;

import java.util.Iterator;

class SpringConvertedResult<R> implements org.springframework.data.neo4j.conversion.ConvertedResult<R> {
    private final ConvertedResult<R> result;

    public SpringConvertedResult(ConvertedResult<R> result) {
        this.result = result;
    }

    @Override
    public R single() {
        return result.single();
    }

    @Override
    public void handle(final org.springframework.data.neo4j.conversion.Handler<R> rHandler) {
        result.handle(new SpringHandler<R>(rHandler));
    }

    @Override
    public Iterator<R> iterator() {
        return result.iterator();
    }

}
