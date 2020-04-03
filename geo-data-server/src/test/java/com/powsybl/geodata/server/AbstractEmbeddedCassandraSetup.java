package com.powsybl.geodata.server;

import com.github.nosan.embedded.cassandra.api.connection.CqlSessionCassandraConnection;
import com.github.nosan.embedded.cassandra.api.cql.CqlDataSet;
import com.github.nosan.embedded.cassandra.spring.test.EmbeddedCassandra;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@EmbeddedCassandra(scripts = {"classpath:create_keyspace.cql", "classpath:geo_data.cql"})
public abstract class AbstractEmbeddedCassandraSetup {

    @Autowired
    private CqlSessionCassandraConnection cqlSessionCassandraConnection;

    @Before
    public void setup() throws IOException {
        CqlDataSet.ofClasspaths("truncate.cql").forEachStatement(cqlSessionCassandraConnection::execute);
    }

}
