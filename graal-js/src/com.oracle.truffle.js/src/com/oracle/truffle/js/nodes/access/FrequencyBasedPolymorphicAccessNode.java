/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

abstract class FrequencyBasedPolymorphicAccessNode<T extends PropertyCacheNode<?>> extends JavaScriptBaseNode {

    /**
     * Do not start specializing until we've sampled at least this many times.
     */
    private static final short MIN_SAMPLED_ACCESSES = 100;
    /**
     * Stop profiling after this many total hits to ensure cache stability.
     *
     * Should not be much higher than the compilation threshold. Also bounds the size of
     * {@link #hitsDistributionMap}.
     */
    private static final short MAX_SAMPLED_ACCESSES = 1000;

    private int totalHits;
    private short maxHitsPerKey;
    private final short size;
    private Map<Object, HitsCount> hitsDistributionMap = new HashMap<>();

    private FrequencyBasedPolymorphicAccessNode(short size) {
        this.size = size;
        this.hitsDistributionMap = size == 0 ? null : new HashMap<>();
    }

    protected abstract T[] getHighFrequencyNodes();

    protected abstract void setHighFrequencyNode(int position, Object key);

    protected final void interpreterSample(Object key) {
        if (hitsDistributionMap == null) {
            // already bailed out, no need to acquire the lock
            return;
        }
        CompilerAsserts.neverPartOfCompilation();
        final Lock lock = getLock();
        lock.lock();
        try {
            var hitsMap = hitsDistributionMap;
            if (hitsMap == null) {
                // already bailed out
                return;
            }
            int totalHitCount = totalHits;
            if (totalHitCount >= MAX_SAMPLED_ACCESSES) {
                stopSampling();
                return;
            }
            totalHits = totalHitCount + 1;

            assert JSRuntime.isPropertyKey(key);
            HitsCount hitsCounter = hitsMap.computeIfAbsent(key, k -> new HitsCount());
            short hits = hitsCounter.incrementAndGet();
            if (totalHitCount < MIN_SAMPLED_ACCESSES) {
                // Only collect statistics until we have enough frequency data.
                maxHitsPerKey = (short) Math.max(maxHitsPerKey, hits);
                return;
            } else if (totalHitCount == MIN_SAMPLED_ACCESSES && maxHitsPerKey <= 1) {
                // If we still haven't ever seen a key twice until now, give up prematurely.
                // Rationale: If we've already collected, say, 100 unique keys, chances are,
                // we won't be seeing a high-frequency key any time soon either.
                stopSampling();
                return;
            }
            if (!hitsCounter.isCached()) {
                if (hits * (size + 1) > totalHitCount) {
                    // Found a key with a frequency > 1/(size+1), e.g. size 5: >16.6%.
                    // Consider it a high-frequency key and insert it to the cache.
                    T[] highFrequencyNodes = getHighFrequencyNodes();
                    for (int i = 0; i < highFrequencyNodes.length; i++) {
                        if (highFrequencyNodes[i] == null) {
                            hitsCounter.setCached();
                            setHighFrequencyNode(i, key);
                            if (i == highFrequencyNodes.length - 1) {
                                // all cache slots are occupied now, so stop sampling already
                                stopSampling();
                            }
                            return;
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** Stop sampling and clean up. */
    private void stopSampling() {
        CompilerAsserts.neverPartOfCompilation();
        hitsDistributionMap = null;
    }

    private static final class HitsCount {
        private short hits;
        private boolean cached;

        HitsCount() {
        }

        public short incrementAndGet() {
            return ++(this.hits);
        }

        public boolean isCached() {
            return this.cached;
        }

        public void setCached() {
            this.cached = true;
        }

        @Override
        public String toString() {
            return hits + (cached ? " [cached]" : "");
        }
    }

    public static final class FrequencyBasedPropertySetNode extends FrequencyBasedPolymorphicAccessNode<PropertySetNode> {

        @Children private PropertySetNode[] highFrequencyKeys;

        protected final boolean setOwn;
        protected final boolean strict;
        protected final boolean superProperty;

        private static final FrequencyBasedPropertySetNode DISABLED = new FrequencyBasedPropertySetNode(false, false, false, (short) 0);

        @NeverDefault
        public static FrequencyBasedPropertySetNode create(JSContext context, boolean setOwn, boolean isStrict, boolean superProperty) {
            short size = context.getLanguageOptions().frequencyBasedPropertyCacheLimit();
            if (size == 0) {
                return DISABLED;
            }
            return new FrequencyBasedPropertySetNode(setOwn, isStrict, superProperty, size);
        }

        private FrequencyBasedPropertySetNode(boolean setOwn, boolean isStrict, boolean superProperty, short size) {
            super(size);
            this.setOwn = setOwn;
            this.strict = isStrict;
            this.superProperty = superProperty;
            this.highFrequencyKeys = new PropertySetNode[size];
        }

        @Override
        protected PropertySetNode[] getHighFrequencyNodes() {
            return highFrequencyKeys;
        }

        @Override
        protected void setHighFrequencyNode(int position, Object key) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            highFrequencyKeys[position] = insert(PropertySetNode.createImpl(key, false, getJSContext(), strict, setOwn, JSAttributes.getDefault(), false, superProperty));
        }

        public boolean executeFastSet(JSDynamicObject target, Object key, Object value, Object receiver, TruffleString.EqualNode equalsNode) {
            if (setOwn) {
                return false;
            }
            if (CompilerDirectives.inInterpreter()) {
                interpreterSample(key);
            }
            return compiledSet(target, key, value, receiver, equalsNode);
        }

        @ExplodeLoop(kind = FULL_UNROLL_UNTIL_RETURN)
        private boolean compiledSet(JSDynamicObject target, Object key, Object value, Object receiver, TruffleString.EqualNode equalsNode) {
            for (PropertySetNode highFrequencyKey : highFrequencyKeys) {
                if (highFrequencyKey == null) {
                    // subsequent slots must be null, too, since we fill in order and never remove.
                    break;
                }
                if (JSRuntime.propertyKeyEquals(equalsNode, highFrequencyKey.getKey(), key)) {
                    highFrequencyKey.setValue(target, value, receiver);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isAdoptable() {
            return this != DISABLED;
        }
    }

    public static final class FrequencyBasedPropertyGetNode extends FrequencyBasedPolymorphicAccessNode<PropertyGetNode> {

        @Children private PropertyGetNode[] highFrequencyKeys;

        private static final FrequencyBasedPropertyGetNode DISABLED = new FrequencyBasedPropertyGetNode((short) 0);

        @NeverDefault
        public static FrequencyBasedPropertyGetNode create(JSContext context) {
            short size = context.getLanguageOptions().frequencyBasedPropertyCacheLimit();
            if (size == 0) {
                return DISABLED;
            }
            return new FrequencyBasedPropertyGetNode(size);
        }

        private FrequencyBasedPropertyGetNode(short size) {
            super(size);
            this.highFrequencyKeys = new PropertyGetNode[size];
        }

        @Override
        protected PropertyGetNode[] getHighFrequencyNodes() {
            return highFrequencyKeys;
        }

        @Override
        protected void setHighFrequencyNode(int position, Object key) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            highFrequencyKeys[position] = insert(PropertyGetNode.create(key, getJSContext()));
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
                if (highFrequencyKey == null) {
                    // subsequent slots must be null, too, since we fill in order and never remove.
                    break;
                }
                if (JSRuntime.propertyKeyEquals(equalsNode, highFrequencyKey.getKey(), key)) {
                    return highFrequencyKey.getValueOrDefault(target, null);
                }
            }
            return null;
        }

        @Override
        public boolean isAdoptable() {
            return this != DISABLED;
        }
    }
}
