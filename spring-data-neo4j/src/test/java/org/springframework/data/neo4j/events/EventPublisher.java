/*
 * Copyright 2011-2019 the original author or authors.
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
package org.springframework.data.neo4j.events;

import org.neo4j.ogm.session.event.Event;
import org.neo4j.ogm.session.event.EventListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Created by markangrish on 22/09/2016.
 */
@Component
public class EventPublisher extends EventListenerAdapter {

	@Autowired private ApplicationEventPublisher publisher;

	@Override
	public void onPreSave(Event event) {
		this.publisher.publishEvent(new PreSaveEvent(event));
	}

	@Override
	public void onPostSave(Event event) {
		this.publisher.publishEvent(new PostSaveEvent(event));
	}

	@Override
	public void onPreDelete(Event event) {
		this.publisher.publishEvent(new PreDeleteEvent(event));
	}

	@Override
	public void onPostDelete(Event event) {
		this.publisher.publishEvent(new PostDeleteEvent(event));
	}
}
