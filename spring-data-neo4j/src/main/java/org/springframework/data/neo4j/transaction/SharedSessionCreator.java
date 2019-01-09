/*
 * Copyright 2011-2019 the original author or authors.
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

package org.springframework.data.neo4j.transaction;

import static org.neo4j.ogm.transaction.Transaction.Status.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Delegate for creating a shareable Neo4j OGM {@link Session} reference for a given {@link SessionFactory}.
 * <p>
 * A shared Session will behave just like a Session fetched from an application SessionFactory. It will delegate all
 * calls to the current transactional Session, if any; otherwise it will fall back to a newly created Session per
 * operation.
 *
 * @author Mark Angrish
 * @see Neo4jTransactionManager
 */
public class SharedSessionCreator {

	private static final Set<String> transactionRequiringMethods = new HashSet<>(4);

	static {
		transactionRequiringMethods.add("deleteAll");
		transactionRequiringMethods.add("save");
		transactionRequiringMethods.add("delete");
		transactionRequiringMethods.add("purgeDatabase");
	}

	/**
	 * Create a transactional Session proxy for the given SessionFactory.
	 *
	 * @param sessionFactory SessionFactory to obtain Sessions from as needed {@code createSession} call (may be
	 *          {@code null}) Session. Allows the addition or specification of proprietary interfaces.
	 * @return a shareable transactional Session proxy
	 */
	public static Session createSharedSession(SessionFactory sessionFactory) {

		return (Session) Proxy.newProxyInstance(SharedSessionCreator.class.getClassLoader(),
				new Class<?>[] { Session.class }, new SharedSessionInvocationHandler(sessionFactory));
	}

	/**
	 * Invocation handler that delegates all calls to the current transactional Session, if any; else, it will fall back
	 * to a newly created Session per operation.
	 */
	private static class SharedSessionInvocationHandler implements InvocationHandler {

		private final Logger logger = LoggerFactory.getLogger(getClass());

		private final SessionFactory targetFactory;
		private final ClassLoader proxyClassLoader;

		public SharedSessionInvocationHandler(SessionFactory target) {
			this.targetFactory = target;
			this.proxyClassLoader = this.targetFactory.getClass().getClassLoader();
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on Session interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			} else if (method.getName().equals("hashCode")) {
				// Use hashCode of Session proxy.
				return hashCode();
			} else if (method.getName().equals("toString")) {
				// Deliver toString without touching a target Session.
				return "Shared Session proxy for target factory [" + targetFactory + "]";
			} else if (method.getName().equals("beginTransaction")) {
				throw new IllegalStateException(
						"Not allowed to create transaction on shared Session - " + "use Spring transactions instead");
			}

			// Determine current Session: either the transactional one
			// managed by the factory or a temporary one for the given invocation.
			Session target = SessionFactoryUtils.getSession(this.targetFactory);

			if (transactionRequiringMethods.contains(method.getName())) {
				if (target == null
						|| (!TransactionSynchronizationManager.isActualTransactionActive() && target.getTransaction() != null
								&& EnumSet.of(CLOSED, COMMITTED, ROLLEDBACK).contains(target.getTransaction().status()))) {
					throw new IllegalStateException("No Session with actual transaction available "
							+ "for current thread - cannot reliably process '" + method.getName() + "' call");
				}
			}

			// Regular Session operations.
			boolean isNewSession = false;
			if (target == null) {
				logger.debug("Creating new Session for shared Session invocation");
				target = this.targetFactory.openSession();
				isNewSession = true;
			}

			// Invoke method on current Session.
			try {

				return method.invoke(target, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			} finally {
				if (isNewSession) {
					SessionFactoryUtils.closeSession(target);
				}
			}
		}
	}
}
