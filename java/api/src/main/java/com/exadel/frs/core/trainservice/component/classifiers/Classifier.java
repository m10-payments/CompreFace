/*
 * Copyright (c) 2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.exadel.frs.core.trainservice.component.classifiers;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.lang.NonNull;

public interface Classifier extends Serializable {

    List<Pair<Double, String>> predict(double[] input, String apiKey, int resultCount);

    Double verify(double[] input, String apiKey, UUID embeddingId);

    @NonNull
    List<Pair<UUID, Double>> verifySubject(
            @NonNull final String apiKey,
            @NonNull final double[] input,
            @NonNull final String subjectName,
            final int resultCount
    );

    double[] verify(double[] sourceImageEmbedding, double[][] targetImageEmbedding);
}