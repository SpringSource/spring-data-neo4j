/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.repository.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.auditing.config.AuditingConfiguration;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.neo4j.annotation.EnableNeo4jAuditing;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;

/**
 * @author Frantisek Hartman
 */
public class Neo4jAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableNeo4jAuditing.class;
	}

	@Override
	protected String getAuditingHandlerBeanName() {
		return "neo4jAuditingHandler";
	}

	@Override
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(AuditingConfiguration configuration) {

		Assert.notNull(configuration, "AuditingConfiguration must not be null!");

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(Neo4jIsNewAwareAuditingHandler.class);

		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(Neo4jMappingContextFactoryBean.class);
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);

		builder.addConstructorArgValue(definition.getBeanDefinition());
		return configureDefaultAuditHandlerAttributes(configuration, builder);
	}

	@Override
	protected void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
													   BeanDefinitionRegistry registry) {

		Assert.notNull(auditingHandlerDefinition, "BeanDefinition must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

		BeanDefinitionBuilder listenerBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(Neo4jAuditingEventListener.class);
		listenerBeanDefinitionBuilder
				.addConstructorArgValue(ParsingUtils.getObjectFactoryBeanDefinition(getAuditingHandlerBeanName(), registry))
				.addConstructorArgReference("sessionFactory");

		registerInfrastructureBeanWithId(listenerBeanDefinitionBuilder.getBeanDefinition(),
				Neo4jAuditingEventListener.class.getName(), registry);
	}

}
