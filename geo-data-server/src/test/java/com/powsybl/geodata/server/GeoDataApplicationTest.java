package com.powsybl.geodata.server;

import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionTestExecutionListener;
import org.cassandraunit.spring.CassandraUnitTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {GeoDataApplication.class})
@TestExecutionListeners(listeners = {CassandraUnitDependencyInjectionTestExecutionListener.class,
        CassandraUnitTestExecutionListener.class},
        mergeMode = MERGE_WITH_DEFAULTS)
@CassandraDataSet(value = "geo_data.cql", keyspace = CassandraConstants.KEYSPACE_GEO_DATA)
@EmbeddedCassandra
public class GeoDataApplicationTest {

    @MockBean
    GeoDataService geoDataService;

    @Test
    public void main() {
        GeoDataApplication.main(new String[] {});
    }
}
