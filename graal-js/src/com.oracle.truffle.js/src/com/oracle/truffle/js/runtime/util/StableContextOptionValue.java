/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.util;

import java.util.Objects;
import java.util.function.Function;

import org.graalvm.options.OptionKey;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSRealm;

/**
 * A wrapper around a context option that may be treated as stable, and optionally patchable.
 * Certain restrictions apply to how these options may be used, and care must be taken not to
 * sidestep these restrictions to ensure option-dependent behavior is preserved, namely:
 *
 * Stable options must not be used during language initialization and must not be stored anywhere
 * else or affect how JS code is parsed and how nodes are created. Dependent compiled code will be
 * invalidated and interpreted code must observe the new option value immediately.
 *
 * Patchable options must either not be used during context initialization at all or carefully be
 * used in such a way that context patching will leave the context not observably different from a
 * newly created context, as if reversing and reapplying the effects of the option.
 *
 * @see JSContextOptions
 */
public final class StableContextOptionValue<T> {

    private @CompilationFinal Assumption stableAssumption;
    private @CompilationFinal T stableValue;
    private final Function<JSContextOptions, T> getter;
    private final OptionKey<T> optionKey;
    private final String optionName;

    private static final Assumption ASSUMPTION_UNINITIALIZED = Assumption.NEVER_VALID;

    public StableContextOptionValue(Function<JSContextOptions, T> getter, OptionKey<T> optionKey, String optionName) {
        this.stableAssumption = ASSUMPTION_UNINITIALIZED;
        this.stableValue = optionKey.getDefaultValue();
        this.getter = getter;
        this.optionKey = optionKey;
        this.optionName = optionName;
    }

    public T get() {
        assert isInitialized() : "Stable context option " + optionName + " accessed before initialization.";
        if (stableAssumption.isValid()) {
            assert Objects.equals(stableValue, getFromContext());
            return stableValue;
        } else {
            return getFromContext();
        }
    }

    public T getFromContext() {
        return getter.apply(JSRealm.get(null).getContextOptions());
    }

    private boolean isInitialized() {
        return stableAssumption != ASSUMPTION_UNINITIALIZED;
    }

    public enum UpdateKind {
        INITIALIZE,
        UPDATE,
        PATCH,
    }

    public void update(JSContextOptions contextOptions, UpdateKind kind) {
        T newValue = getter.apply(contextOptions);
        switch (kind) {
            case INITIALIZE -> setInitialValue(newValue);
            case UPDATE -> updateValue(newValue);
            case PATCH -> patchValue(newValue);
        }
    }

    private synchronized void setInitialValue(T initialValue) {
        CompilerAsserts.neverPartOfCompilation();
        assert !isInitialized();
        assert Objects.equals(stableValue, optionKey.getDefaultValue());
        stableValue = initialValue;
        stableAssumption = makeAssumption();
    }

    private synchronized void updateValue(T newValue) {
        CompilerAsserts.neverPartOfCompilation();
        assert isInitialized();
        T oldValue = stableValue;
        if (!Objects.equals(oldValue, newValue)) {
            invalidateAssumption(newValue, oldValue);
        }
    }

    private synchronized void patchValue(T newValue) {
        CompilerAsserts.neverPartOfCompilation();
        if (!isInitialized()) {
            setInitialValue(newValue);
        } else {
            // Stable value has already been initialized, cannot patch.
            updateValue(newValue);
        }
    }

    private Assumption makeAssumption() {
        return Assumption.create(optionName);
    }

    private void invalidateAssumption(T newValue, T oldValue) {
        Assumption oldAssumption = stableAssumption;
        if (oldAssumption.isValid()) {
            oldAssumption.invalidate(String.format("Option %s was changed from %s to %s.", optionName, oldValue, newValue));
        }
    }

    @Override
    public int hashCode() {
        return optionKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj instanceof StableContextOptionValue<?> other && other.optionKey == this.optionKey;
    }
}
