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

package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.index.impl.lucene.QueryNotPossibleException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.impl.core.ReadOnlyDbException;
import org.neo4j.kernel.impl.nioneo.store.StoreFailureException;
import org.neo4j.kernel.impl.persistence.IdGenerationFailedException;
import org.neo4j.kernel.impl.transaction.IllegalResourceException;
import org.neo4j.kernel.impl.transaction.LockException;
import org.springframework.dao.*;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.mapping.InvalidEntityTypeException;

/**
 * @author mh
 * @since 21.02.11
 */
public class Neo4jExceptionTranslator implements PersistenceExceptionTranslator {

    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        // todo delete, duplicate semantics
        try {
            throw ex;
        } catch(IllegalArgumentException iae) {
            if (iae.getCause() != null && iae.getCause() instanceof InvalidEntityTypeException) {
                throw (InvalidEntityTypeException)iae.getCause();
            }
            throw new InvalidDataAccessApiUsageException(iae.getMessage(),iae);
        } catch(DataAccessException dae) {
            throw dae;
        } catch(NotInTransactionException nit) {
            throw new InvalidDataAccessApiUsageException(nit.getMessage(), nit);
        } catch(TransactionFailureException tfe) {
            throw new org.springframework.data.neo4j.core.UncategorizedGraphStoreException(tfe.getMessage(), tfe);
        } catch(ReadOnlyDbException rodbe) {
            throw new InvalidDataAccessResourceUsageException(rodbe.getMessage(), rodbe);
        } catch(IllegalResourceException ire) {
            throw new InvalidDataAccessResourceUsageException(ire.getMessage(), ire);
        } catch(StoreFailureException sfe) {
            throw new DataAccessResourceFailureException(sfe.getMessage(), sfe);
        } catch(IdGenerationFailedException idfe) {
            throw new NonTransientDataAccessResourceException(idfe.getMessage(), idfe);
        } catch(NotFoundException nfe) {
            throw new DataRetrievalFailureException(nfe.getMessage(), nfe);
        } catch(QueryNotPossibleException qnpe) {
            throw new ConcurrencyFailureException(qnpe.getMessage(),qnpe);
        } catch(DeadlockDetectedException dde) {
            throw new ConcurrencyFailureException(dde.getMessage(),dde);
        } catch(LockException le) {
            throw new ConcurrencyFailureException(le.getMessage(),le);
        } catch(RuntimeException e) {
            throw e; // exception thrown by the user
        }
    }

}
