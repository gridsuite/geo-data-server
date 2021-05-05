/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.test;

import com.github.nosan.embedded.cassandra.EmbeddedCassandraFactory;
import com.github.nosan.embedded.cassandra.api.Cassandra;
import com.github.nosan.embedded.cassandra.api.CassandraFactory;
import com.github.nosan.embedded.cassandra.api.Version;
import com.github.nosan.embedded.cassandra.api.cql.CqlScript;
import com.github.nosan.embedded.cassandra.artifact.RemoteArtifact;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.time.Duration;

import com.github.nosan.embedded.cassandra.api.cql.CqlDataSet;
import com.github.nosan.embedded.cassandra.api.connection.CassandraConnection;
import com.github.nosan.embedded.cassandra.api.connection.DefaultCassandraConnectionFactory;
import com.github.nosan.embedded.cassandra.commons.io.ClassPathResource;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Configuration
public class EmbeddedCassandraFactoryConfig {

    @Value("${cassandra-keyspace:geo_data}")
    private String keyspaceName;

    @Bean(initMethod = "start", destroyMethod = "stop")
    Cassandra cassandra(CassandraFactory cassandraFactory) {
        return cassandraFactory.create();
    }

    @Bean
    CassandraConnection cassandraConnection(Cassandra cassandra) {
        CassandraConnection cassandraConnection = new DefaultCassandraConnectionFactory().create(cassandra);
        String[] createKeyspace = CqlScript.ofClasspath("create_keyspace.cql").getStatements().stream().map(s -> String.format(s, keyspaceName)).toArray(l -> new String[l]);
        CqlDataSet.ofStrings(createKeyspace).add(CqlDataSet.ofStrings(String.format("USE %s;", keyspaceName))).add(CqlDataSet.ofClasspaths("geo_data.cql")).forEachStatement(cassandraConnection::execute);
        return cassandraConnection;
    }

    @Bean
    @Scope("singleton")
    CassandraFactory embeddedCassandraFactory() throws UnknownHostException {
        EmbeddedCassandraFactory cassandraFactory = new EmbeddedCassandraFactory();
        RemoteArtifact artifact = new RemoteArtifact(Version.of("4.0-alpha4"));
        String proxyHost = System.getProperty("https.proxyHost", System.getProperty("http.proxyHost", System.getProperty("proxyHost")));
        if (proxyHost != null && !proxyHost.isEmpty()) {
            String proxyPort = System.getProperty("https.proxyPort", System.getProperty("http.proxyPort", System.getProperty("proxyPort")));
            String proxyUser = System.getProperty("https.proxyUser", System.getProperty("http.proxyUser", System.getProperty("proxyUser")));
            if (proxyUser != null && !proxyUser.isEmpty()) {
                String proxyPassword = System.getProperty("https.proxyPassword", System.getProperty("http.proxyPassword", System.getProperty("proxyPassword")));
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestorType() == RequestorType.PROXY) {
                            String prot = getRequestingProtocol().toLowerCase();
                            String host = System.getProperty(prot + ".proxyHost", proxyHost);
                            String port = System.getProperty(prot + ".proxyPort", proxyPort);
                            String user = System.getProperty(prot + ".proxyUser", proxyUser);
                            String password = System.getProperty(prot + ".proxyPassword", proxyPassword);
                            if (getRequestingHost().equalsIgnoreCase(host)) {
                                if (Integer.parseInt(port) == getRequestingPort()) {
                                    return new PasswordAuthentication(user, password.toCharArray());
                                }
                            }
                        }
                        return null;
                    }
                });
            }
            artifact.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))));
        }
        cassandraFactory.setArtifact(artifact);
        cassandraFactory.setConfig(new ClassPathResource("cassandra.yaml"));
        cassandraFactory.setPort(9142);
        cassandraFactory.setJmxLocalPort(0);
        cassandraFactory.setRpcPort(0);
        cassandraFactory.setStoragePort(16432);
        cassandraFactory.setAddress(InetAddress.getByName("localhost"));
        cassandraFactory.setTimeout(Duration.ofSeconds(900)); //default is 90, we are getting timeouts on GH actions
        return cassandraFactory;
    }
}