/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

export var moduleBlock = (async function() {

    var kMeans = module {

        const N = 100000; // datapoints
        const K = 7; // cluster
        const SIZE = 600 // max data size

        const N_EXP = 20; // 20 runs

        const clusterX = new Array(K);
        const clusterY = new Array(K);

        const dataPointsX = new Array(N);
        const dataPointsY = new Array(N);
        const dataPointsCluster = new Array(N);

        function computeDist(x1, y1, x2, y2) {
            let dx = x1-x2;
            let dy = y1-y2;

            return dx * dx + dy * dy;
        }

        function getClosestCluster(x,y) {
            let minCluster=-1;
            let minDist = Number.MAX_VALUE; // set min to max dist

            for (let i = 0; i < K; i++) {
                let dist = computeDist(x,y,clusterX[i],clusterY[i]);

                if (dist < minDist) {
                    minCluster=i;
                    minDist = dist;
                }
            }

            return minCluster;
        }

        function computeCentroids() {
            let sumX = new Array(K);
            let sumY = new Array(K);
            let sumN = new Array(K);

            for (let i = 0; i < N; i++) {
                sumX[dataPointsCluster[i]] += dataPointsX[i];
                sumY[dataPointsCluster[i]] += dataPointsY[i];
                sumN[dataPointsCluster[i]]++;
            }

            for (let i = 0; i < K; i++) {
                if (sumN[i] != 0) {
                    clusterX[i] = sumX[i] / sumN[i];
                    clusterY[i] = sumY[i] / sumN[i];
                }
            }
        }

        function doNewClustering() {
            let stable = true;

            for (let i = 0; i < N; i++) {
                let closestCluster = getClosestCluster(dataPointsX[i],dataPointsY[i]);

                if (stable && dataPointsCluster[i] != closestCluster) {
                    stable = false;
                }

                dataPointsCluster[i] = closestCluster;
            }

            return stable;
        }

        function doInitialClustering() {
            for (let i = 0; i < N; i++) {
                dataPointsCluster[i] = Math.floor(Math.random() * K);
            }
        }

        function createRandomData() {
            for (let i = 0; i < N; i++) {
                dataPointsX[i] = Math.floor(Math.random() * SIZE);
                dataPointsY[i] = Math.floor(Math.random() * SIZE);
            }
        }

        function cluster() {
            doInitialClustering();

            computeCentroids();

            let running = false;

            while (!running) {
                running = doNewClustering();
                computeCentroids();
            }
        }

        createRandomData();
        cluster();
    };

    return await import(kMeans);
});
