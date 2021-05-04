/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.neo4j.driver.Bookmark;
import org.neo4j.driver.SessionConfig;

/**
 * This is a utility class that captures the most recent bookmarks after any of the Spring Data Neo4j transaction managers
 * commits a transaction. It also can preload the bookmarks;
 *
 * @author Michael J. Simons
 * @soundtrack Black Sabbath - Master Of Reality
 */
public final class BookmarkCapture implements Supplier<Set<Bookmark>>, Consumer<Set<Bookmark>> {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	private Set<Bookmark> latestBookmarks;

	private Bookmark nextBookmark;

	public SessionConfig createSessionConfig() {
		return createSessionConfig(null);
	}

	public SessionConfig createSessionConfig(String databaseName) {
		try {
			read.lock();
			SessionConfig.Builder builder = SessionConfig.builder().withBookmarks(latestBookmarks == null ? Collections.emptyList() : latestBookmarks);
			if (databaseName != null) {
				builder.withDatabase(databaseName);
			}
			return builder.build();
		} finally {
			read.unlock();
		}
	}

	public void fastForwardTo(Bookmark bookmark) {

		try {
			write.lock();
			nextBookmark = bookmark;
		} finally {
			write.unlock();
		}
	}

	@Override
	public void accept(Set<Bookmark> bookmarks) {
		try {
			write.lock();
			latestBookmarks = bookmarks;
		} finally {
			write.unlock();
		}
	}

	@Override
	public Set<Bookmark> get() {
		try {
			read.lock();
			return nextBookmark == null ? Collections.emptySet() : Collections.singleton(nextBookmark);
		} finally {
			read.unlock();
		}
	}
}
