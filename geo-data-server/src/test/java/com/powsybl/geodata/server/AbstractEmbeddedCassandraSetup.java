/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server;

import com.github.nosan.embedded.cassandra.api.connection.ClusterCassandraConnection;
import com.github.nosan.embedded.cassandra.api.cql.CqlDataSet;
import com.github.nosan.embedded.cassandra.spring.test.EmbeddedCassandra;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@EmbeddedCassandra(scripts = {"classpath:create_keyspace.cql", "classpath:geo_data.cql"})
public abstract class AbstractEmbeddedCassandraSetup {

    @Autowired
    private ClusterCassandraConnection clusterCassandraConnection;

    @Before
    public void setup() {
        CqlDataSet.ofClasspaths("truncate.cql").forEachStatement(clusterCassandraConnection::execute);
    }

}
