package com.powsybl.geodata.server;

import com.github.nosan.embedded.cassandra.EmbeddedCassandraFactory;
import com.github.nosan.embedded.cassandra.api.CassandraFactory;
import com.github.nosan.embedded.cassandra.api.Version;
import com.github.nosan.embedded.cassandra.artifact.DefaultArtifact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class EmbeddedCassandraFactoryConfig {

    @Bean
    @Scope("singleton")
    CassandraFactory embeddedCassandraFactory() throws UnknownHostException {
        EmbeddedCassandraFactory cassandraFactory = new EmbeddedCassandraFactory();
        Version version = Version.of("4.0-alpha3");
        Path directory = Paths.get("/apache-cassandra-4.0-alpha3");
        cassandraFactory.setArtifact(new DefaultArtifact(version, directory));
        cassandraFactory.setPort(9142);
        cassandraFactory.setAddress(InetAddress.getByName("localhost"));
        return cassandraFactory;
    }
}
