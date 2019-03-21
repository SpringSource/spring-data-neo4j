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

package org.springframework.data.neo4j.rest.support;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.rest.graphdb.ExecutingRestRequest;
import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.server.NeoServer;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.rest.SpringCypherRestGraphDatabase;
import org.springframework.data.neo4j.rest.SpringCypherRestGraphDatabase;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class RestTestBase {

    protected static ImpermanentGraphDatabase db;
    protected GraphDatabase restGraphDatabase;
    private static final String HOSTNAME = "127.0.0.1";
    public static final int PORT = 7470;
    protected static NeoServer neoServer = null;
    public static final String SERVER_ROOT_URI = "http://" + HOSTNAME + ":" + PORT + "/db/data/";
    private Node refNode;

    @BeforeClass
    public static void startDb() throws Exception {
        db = new ImpermanentGraphDatabase();
        final ServerConfigurator configurator = new ServerConfigurator(db);
        configurator.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY,PORT);
        configurator.configuration().setProperty("dbms.security.auth_enabled",false);
        final WrappingNeoServerBootstrapper bootstrapper = new WrappingNeoServerBootstrapper(db, configurator);
        bootstrapper.start();
//        if (exit != 0 ) throw new IllegalStateException("Server not started correctly.");
        neoServer = bootstrapper.getServer();

        tryConnect();
    }

    private static void tryConnect() throws InterruptedException {
        int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                RequestResult result = new ExecutingRestRequest(SERVER_ROOT_URI).get("");
                assertEquals(200, result.getStatus());
                System.err.println("Successful HTTP connection to "+SERVER_ROOT_URI);
                return;
            } catch (Exception e) {
                System.err.println("Error retrieving ROOT URI " + e.getMessage());
                Thread.sleep(500);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        cleanDb();
//        restGraphDatabase = new SpringRestGraphDatabase(SERVER_ROOT_URI);
        restGraphDatabase = new SpringCypherRestGraphDatabase(SERVER_ROOT_URI);
        refNode = createNode();
    }

    @After
    public void tearDown() throws Exception {
        if (restGraphDatabase!=null) restGraphDatabase.shutdown();
    }

    public Node createNode() {
        return restGraphDatabase.createNode(null,null);
    }

    public static void cleanDb() {
        new Neo4jDatabaseCleaner(db).cleanDb();
        //db.cleanContent(true);
    }

    @AfterClass
    public static void shutdownDb() {
        neoServer.stop();
    }

    public GraphDatabaseService getGraphDatabase() {
        return db;
    }


    protected Relationship relationship() {
        Iterator<Relationship> it = node().getRelationships(Direction.OUTGOING).iterator();
        if (it.hasNext()) return it.next();
        return node().createRelationshipTo(createNode(), Type.TEST);
    }

    protected IndexManager index() {
        return ((GraphDatabaseService)restGraphDatabase).index();
    }

    protected Node node() {
        return refNode;
    }
}
