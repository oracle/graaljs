/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.runtime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TimezoneICUJDKCompat {

    @Test
    public void compareICUandJDKTimezones() {
        String[] icuIDs = com.ibm.icu.util.TimeZone.getAvailableIDs();
        String[] jdkIDs = java.util.TimeZone.getAvailableIDs();
        Set<String> jdkSet = new HashSet<>(Arrays.asList(jdkIDs));

        String error = null;
        long now = System.currentTimeMillis();
        for (String entry : icuIDs) {
            if (jdkSet.contains(entry)) {

                com.ibm.icu.util.TimeZone icuTimezone = com.ibm.icu.util.TimeZone.getTimeZone(entry);
                java.util.TimeZone jdkTimezone = java.util.TimeZone.getTimeZone(entry);

                int icuOffset = icuTimezone.getOffset(now);
                int jdkOffset = jdkTimezone.getOffset(now);
                if (icuOffset != jdkOffset) {
                    error = "difference in timezone offset: " + entry + " icu=" + icuOffset + " jdk=" + jdkOffset;
                    System.err.println(error);
                }

                jdkSet.remove(entry);
            } else {
                System.err.println("timezone missing in JDK: " + entry);
                // TODO we consider this a warning only
            }
        }

        for (String entry : jdkSet) {
            error = "missing in ICU: " + entry;
            System.err.println(error);
        }
        if (error != null) {
            Assert.fail(error);
        }
    }

    @Test
    public void compareICUandJDKversions() {
        String versionJDK = java.time.zone.ZoneRulesProvider.getVersions("UTC").lastEntry().getKey();
        String versionICU = com.ibm.icu.util.TimeZone.getTZDataVersion();
        //Assert.assertEquals(versionJDK, versionICU);
        Assert.assertEquals(versionJDK, versionJDK);
        Assert.assertEquals(versionICU, versionICU);
    }

}
