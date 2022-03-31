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
package com.oracle.truffle.js.nodes.access;

import static com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind.FULL_UNROLL_UNTIL_RETURN;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

abstract class FrequencyBasedPolymorphicAccessNode<T extends PropertyCacheNode<?>> extends JavaScriptBaseNode {

    // Do not cache keys used less than this perc. of total accesses (interpreter only).
    private static final int MIN_CACHING_PERC = 10;
    // Do not sample more than this number of keys (interpreter only).
    private static final int MAX_DISTRIBUTION_MAP_SIZE = 1024;
    // Do not cache locations accessed less than this value (interpreter only).
    private static final int MIN_KEYS_ACCESSES = 100;
    // Compute distribution statistics every XX accesses (e.g., every 10 reads).
    private static final int SAMPLE_EVERY = 10;
    // Max size of ICs.
    private static final int IC_GET_MAX_SIZE = 5;
    private static final int IC_SET_MAX_SIZE = 3;

    public static FrequencyBasedPropertyGetNode createFrequencyBasedPropertyGet(JSContext context) {
        return FrequencyBasedPropertyGetNode.create(context);
    }

    public static FrequencyBasedPropertySetNode createFrequencyBasedPropertySet(JSContext context, boolean setOwn, boolean strict, boolean superProperty) {
        return FrequencyBasedPropertySetNode.create(context, setOwn, strict, superProperty);
    }

    protected final JSContext context;

    private int totalHits;
    private final int[] topHits;
    private Map<Object, HitsCount> hitsDistributionMap = new HashMap<>();

    private FrequencyBasedPolymorphicAccessNode(JSContext context, int size) {
        this.context = context;
        this.topHits = new int[size];
    }

    protected abstract T[] getHighFrequencyNodes();

    protected abstract void setHighFrequencyNode(int position, Object key);

    protected void interpreterSample(Object key) {
        final Lock lock = getLock();
        lock.lock();
        try {
            CompilerAsserts.neverPartOfCompilation();
            assert JSRuntime.isPropertyKey(key);
            if (hitsDistributionMap == null) {
                return;
            } else if (hitsDistributionMap.size() >= MAX_DISTRIBUTION_MAP_SIZE) {
                // Bailout
                hitsDistributionMap = null;
                return;
            }
            totalHits++;

            HitsCount hitsCounter = hitsDistributionMap.get(key);
            if (hitsCounter == null) {
                hitsCounter = new HitsCount();
                hitsDistributionMap.put(key, hitsCounter);
            }
            int hits = hitsCounter.incrementAndGet();
            if (hits % SAMPLE_EVERY != 0 || totalHits < MIN_KEYS_ACCESSES) {
                // Do not sample on every property access, and ignore keys and locations not used
                // too often.
                return;
            }
            for (int i = 0; i < topHits.length; i++) {
                T[] highFrequencyNodes = getHighFrequencyNodes();
                if (hits > topHits[i]) {
                    if (highFrequencyNodes[i] == null) {
                        // new IC entry
                        setHighFrequencyNode(i, key);
                        topHits[i] = hits;
                        break;
                    } else if (highFrequencyNodes[i].getKey().equals(key)) {
                        // update IC entry at same position
                        topHits[i] = hits;
                        break;
                    } else {
                        // shift IC entries, and insert
                        for (int j = topHits.length - 1; j > i; j--) {
                            highFrequencyNodes[j] = highFrequencyNodes[j - 1];
                            topHits[j] = topHits[j - 1];
                        }
                        setHighFrequencyNode(i, key);
                        topHits[i] = hits;
                        break;
                    }
                }
            }
            // Remove IC entries that are executed less frequently if below % threshold.
            for (int i = 0; i < topHits.length; i++) {
                if (topHits[i] == 0) {
                    break;
                }
                int perc = percentage(topHits[i]);
                if (perc < MIN_CACHING_PERC) {
                    topHits[i] = 0;
                    getHighFrequencyNodes()[i] = null;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private int percentage(int hits) {
        return (int) (((float) hits / (float) totalHits) * 100);
    }

    private static class HitsCount {
        private int hits;

        HitsCount() {
            this.hits = 0;
        }

        public int incrementAndGet() {
            return ++(this.hits);
        }
    }

    public static final class FrequencyBasedPropertySetNode extends FrequencyBasedPolymorphicAccessNode<PropertySetNode> {

        @Children private PropertySetNode[] highFrequencyKeys;

        protected final boolean setOwn;
        protected final boolean strict;
        protected final boolean superProperty;

        public static FrequencyBasedPropertySetNode create(JSContext context, boolean setOwn, boolean isStrict, boolean superProperty) {
            return new FrequencyBasedPropertySetNode(context, setOwn, isStrict, superProperty);
        }

        private FrequencyBasedPropertySetNode(JSContext context, boolean setOwn, boolean isStrict, boolean superProperty) {
            super(context, IC_SET_MAX_SIZE);
            this.setOwn = setOwn;
            this.strict = isStrict;
            this.superProperty = superProperty;
            this.highFrequencyKeys = new PropertySetNode[IC_SET_MAX_SIZE];
        }

        @Override
        protected PropertySetNode[] getHighFrequencyNodes() {
            return highFrequencyKeys;
        }

        @Override
        protected void setHighFrequencyNode(int position, Object key) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            assert JSRuntime.isPropertyKey(key);
            highFrequencyKeys[position] = insert(PropertySetNode.createImpl(key, false, context, strict, setOwn, JSAttributes.getDefault(), false, superProperty));
        }

        public boolean executeFastSet(JSDynamicObject target, Object key, Object value, Object receiver, TruffleString.EqualNode equalsNode) {
            if (setOwn) {
                return false;
            }
            assert JSRuntime.isPropertyKey(key);
            if (CompilerDirectives.inInterpreter()) {
                interpreterSample(key);
            }
            return compiledSet(target, key, value, receiver, equalsNode);
        }

        @ExplodeLoop(kind = FULL_UNROLL_UNTIL_RETURN)
        private boolean compiledSet(JSDynamicObject target, Object key, Object value, Object receiver, TruffleString.EqualNode equalsNode) {
            for (PropertySetNode highFrequencyKey : highFrequencyKeys) {
                if (highFrequencyKey != null && JSRuntime.propertyKeyEquals(equalsNode, highFrequencyKey.getKey(), key)) {
                    highFrequencyKey.setValue(target, value, receiver);
                    return true;
                }
            }
            return false;
        }
    }

    public static final class FrequencyBasedPropertyGetNode extends FrequencyBasedPolymorphicAccessNode<PropertyGetNode> {

        @Children private PropertyGetNode[] highFrequencyKeys;

        public static FrequencyBasedPropertyGetNode create(JSContext context) {
            return new FrequencyBasedPropertyGetNode(context);
        }

        private FrequencyBasedPropertyGetNode(JSContext context) {
            super(context, IC_GET_MAX_SIZE);
            this.highFrequencyKeys = new PropertyGetNode[IC_GET_MAX_SIZE];
        }

        @Override
        protected PropertyGetNode[] getHighFrequencyNodes() {
            return highFrequencyKeys;
        }

        @Override
        protected void setHighFrequencyNode(int position, Object key) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            assert JSRuntime.isPropertyKey(key);
            highFrequencyKeys[position] = insert(PropertyGetNode.create(key, context));
        }

        public Object executeFastGet(Object key, Object target, TruffleString.EqualNode equalsNode) {
            if (CompilerDirectives.inInterpreter()) {
                interpreterSample(key);
            }
            return readFromCaches(key, target, equalsNode);
        }

        @ExplodeLoop(kind = FULL_UNROLL_UNTIL_RETURN)
        private Object readFromCaches(Object key, Object target, TruffleString.EqualNode equalsNode) {
            for (PropertyGetNode highFrequencyKey : highFrequencyKeys) {
                if (highFrequencyKey != null && JSRuntime.propertyKeyEquals(equalsNode, highFrequencyKey.getKey(), key)) {
                    return highFrequencyKey.getValueOrDefault(target, null);
                }
            }
            return null;
        }
    }
}
