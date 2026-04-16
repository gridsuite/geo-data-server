/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import jakarta.annotation.PreDestroy;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * @author Ghazwa Rehili <ghazwa.rehili at rte-france.com>
 */
@Service
public class GeoDataExecutionService {
    private final ExecutorService executorService;

    public GeoDataExecutionService(@Value("${max-concurrent-requests}") int maxConcurrentRequests,
                                   @NonNull GeoDataObserver geoDataObserver) {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxConcurrentRequests);
        geoDataObserver.createThreadPoolMetric(threadPoolExecutor);
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().build();
        executorService = ContextExecutorService.wrap(threadPoolExecutor,
            snapshotFactory::captureAll);
    }

    @PreDestroy
    private void preDestroy() {
        executorService.shutdown();
    }

    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return CompletableFuture.supplyAsync(supplier, executorService);
    }
}
