package com.powsybl.geodata.server.repositories;

import com.powsybl.geodata.extensions.Coordinate;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CoordinateEntityTest {

    @Test
    public void test() {
        List<CoordinateEntity> coordinateEntities =  CoordinateEntity.create(Arrays.asList(new Coordinate(1, 1), new Coordinate(2, 2)));
        assertEquals(2, coordinateEntities.size(), 0);

        CoordinateEntity.CoordinateEntityBuilder coordinateEntityBuilder =  CoordinateEntity.builder().lat(2).lon(2);
        assertEquals("CoordinateEntity.CoordinateEntityBuilder(lat=2.0, lon=2.0)", coordinateEntityBuilder.toString());
        assertEquals("CoordinateEntity(lat=2.0, lon=2.0)", coordinateEntityBuilder.build().toString());
    }
}
