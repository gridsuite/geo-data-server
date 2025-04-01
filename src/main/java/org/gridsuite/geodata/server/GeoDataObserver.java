/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.NonNull;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
@Service
public class GeoDataObserver {

    private static final String OBSERVATION_PREFIX = "app.geo-data.";
    private static final String TASK_POOL_METER_NAME_PREFIX = OBSERVATION_PREFIX + "tasks.";
    private static final String TASK_TYPE_TAG_NAME = "type";
    private static final String TASK_TYPE_TAG_VALUE_CURRENT = "current";
    private static final String TASK_TYPE_TAG_VALUE_PENDING = "pending";
    private static final String GET_LINES_OBSERVATION_NAME = OBSERVATION_PREFIX + "lineGeoData.";
    private static final String GET_SUBSTATIONS_OBSERVATION_NAME = OBSERVATION_PREFIX + "substationGeoData.";
    private static final String NUMBER_LINES_METER_NAME = OBSERVATION_PREFIX + GET_LINES_OBSERVATION_NAME + "count";
    private static final String NUMBER_SUBSTATIONS_METER_NAME = OBSERVATION_PREFIX + GET_SUBSTATIONS_OBSERVATION_NAME + "count";
    private static final String NETWORK_ID_TAG_NAME = "networkId";
    private final ObservationRegistry observationRegistry;
    private final MeterRegistry meterRegistry;

    public GeoDataObserver(@NonNull ObservationRegistry observationRegistry, @NonNull MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.meterRegistry = meterRegistry;
    }

    public <E extends Throwable> List<LineGeoData> observeLinesData(String networkId, Observation.CheckedCallable<List<LineGeoData>, E> callable) throws E {
        Observation observation = createLinesObservation(networkId);
        List<LineGeoData> linesData = observation.observeChecked(callable);
        if (linesData != null) {
            recordLinesCount(networkId, linesData.size());
        }
        return linesData;
    }

    public <E extends Throwable> List<SubstationGeoData> observeSubstationsData(String networkId, Observation.CheckedCallable<List<SubstationGeoData>, E> callable) throws E {
        Observation observation = createSubstationsObservation(networkId);
        List<SubstationGeoData> substationsData = observation.observeChecked(callable);
        if (substationsData != null) {
            recordSubstationsCount(networkId, substationsData.size());
        }
        return substationsData;
    }

    private Observation createLinesObservation(String networkId) {
        return Observation.createNotStarted(GeoDataObserver.GET_LINES_OBSERVATION_NAME, observationRegistry)
                .lowCardinalityKeyValue(NETWORK_ID_TAG_NAME, networkId);
    }

    private Observation createSubstationsObservation(String networkId) {
        return Observation.createNotStarted(GeoDataObserver.GET_SUBSTATIONS_OBSERVATION_NAME, observationRegistry)
                .lowCardinalityKeyValue(NETWORK_ID_TAG_NAME, networkId);
    }

    private void recordLinesCount(String networkId, long count) {
        Counter.builder(GeoDataObserver.NUMBER_LINES_METER_NAME)
                .tag(NETWORK_ID_TAG_NAME, networkId)
                .register(meterRegistry)
                .increment(count);
    }

    private void recordSubstationsCount(String networkId, long count) {
        Counter.builder(GeoDataObserver.NUMBER_SUBSTATIONS_METER_NAME)
                .tag(NETWORK_ID_TAG_NAME, networkId)
                .register(meterRegistry)
                .increment(count);
    }

    public void createThreadPoolMetric(ThreadPoolExecutor threadPoolExecutor) {
        Gauge.builder(TASK_POOL_METER_NAME_PREFIX + TASK_TYPE_TAG_VALUE_CURRENT,
                        threadPoolExecutor, ThreadPoolExecutor::getActiveCount)
                .description("The number of active request tasks in the thread pool")
                .tag(TASK_TYPE_TAG_NAME, TASK_TYPE_TAG_VALUE_CURRENT)
                .register(meterRegistry);

        Gauge.builder(TASK_POOL_METER_NAME_PREFIX + TASK_TYPE_TAG_VALUE_PENDING,
                        threadPoolExecutor, executor -> executor.getQueue().size())
                .description("The number of pending request tasks in the thread pool")
                .tag(TASK_TYPE_TAG_NAME, TASK_TYPE_TAG_VALUE_PENDING)
                .register(meterRegistry);
    }
}
