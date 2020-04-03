package com.powsybl.geodata.server;

import com.github.nosan.embedded.cassandra.api.connection.CqlSessionCassandraConnection;
import com.github.nosan.embedded.cassandra.spring.test.EmbeddedCassandra;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@EmbeddedCassandra(scripts = {"classpath:create_keyspace.cql", "classpath:geo_data.cql"})
public abstract class AbstractEmbeddedCassandraSetup {

    @Autowired
    private CqlSessionCassandraConnection cqlSessionCassandraConnection;

    @Before
    public void setup() throws IOException {
        InputStream truncateScriptIS = getClass().getClassLoader().getResourceAsStream("truncate.cql");
        String truncateScript = IOUtils.toString(truncateScriptIS, StandardCharsets.UTF_8);
        executeScript(truncateScript);
    }

    public void executeScript(String script) {
        String cleanedScript = script.replace("\n", "");
        String[] requests = cleanedScript.split("(?<=;)");
        for (String request : requests) {
            if (!request.equals(" ")) {
                cqlSessionCassandraConnection.execute(request);
            }
        }
    }
}
