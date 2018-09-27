/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.js.runtime.JSContext;

public final class JSFunctionData {

    /** [[Call]]. */
    @CompilationFinal private volatile CallTarget callTarget;
    /** [[Construct]] without {@code newTarget}. */
    @CompilationFinal private volatile CallTarget constructTarget;
    /** [[Construct]] with {@code newTarget}. */
    @CompilationFinal private volatile CallTarget constructNewTarget;

    private final JSContext context;
    @CompilationFinal private String name;
    /** The ExpectedArgumentCount and initial value of the function's {@code length} property. */
    private final int length;

    private final int flags;

    /** If {@code true}, this function has a [[Construct]] internal method. */
    private static final int IS_CONSTRUCTOR = 1 << 0;
    /**
     * Is this a derived class constructor, i.e., [[ConstructorKind]] internal slot is "derived".
     */
    private static final int IS_DERIVED = 1 << 1;
    private static final int IS_STRICT = 1 << 2;
    private static final int IS_BUILTIN = 1 << 3;
    private static final int NEEDS_PARENT_FRAME = 1 << 4;
    private static final int IS_GENERATOR = 1 << 5;
    private static final int IS_ASYNC = 1 << 6;
    /**
     * Class constructors require {@code new} and have a not writable {@code prototype} property.
     */
    private static final int IS_CLASS_CONSTRUCTOR = 1 << 7;
    /** Affects the function object's {@code caller} and {@code arguments} properties. */
    private static final int STRICT_FUNCTION_PROPERTIES = 1 << 8;
    /** Does this function need a newTarget implicit argument when invoked with {@code new}. */
    private static final int NEEDS_NEW_TARGET = 1 << 9;
    /** Is this a bound function. */
    private static final int IS_BOUND = 1 << 10;

    /** Innermost call target used for lazy creation of the actual call targets. */
    private volatile CallTarget rootTarget;
    /** Lazy initialization function. */
    private volatile Initializer lazyInit;

    private static final AtomicReferenceFieldUpdater<JSFunctionData, CallTarget> UPDATER_CALL_TARGET = //
                    AtomicReferenceFieldUpdater.newUpdater(JSFunctionData.class, CallTarget.class, "callTarget");
    private static final AtomicReferenceFieldUpdater<JSFunctionData, CallTarget> UPDATER_CONSTRUCT_TARGET = //
                    AtomicReferenceFieldUpdater.newUpdater(JSFunctionData.class, CallTarget.class, "constructTarget");
    private static final AtomicReferenceFieldUpdater<JSFunctionData, CallTarget> UPDATER_CONSTRUCT_NEW_TARGET = //
                    AtomicReferenceFieldUpdater.newUpdater(JSFunctionData.class, CallTarget.class, "constructNewTarget");
    private static final AtomicReferenceFieldUpdater<JSFunctionData, CallTarget> UPDATER_ROOT_TARGET = //
                    AtomicReferenceFieldUpdater.newUpdater(JSFunctionData.class, CallTarget.class, "rootTarget");

    private JSFunctionData(JSContext context, CallTarget callTarget, CallTarget constructTarget, CallTarget constructNewTarget, int length, String name, int flags) {
        this.context = context;
        this.callTarget = callTarget;
        this.constructTarget = constructTarget;
        this.constructNewTarget = constructNewTarget;
        this.name = name;
        this.length = length;
        this.flags = flags;
    }

    public static JSFunctionData create(JSContext context, CallTarget callTarget, CallTarget constructTarget, CallTarget constructNewTarget, int length, String name, int flags) {
        return new JSFunctionData(context, callTarget, constructTarget, constructNewTarget, length, name, flags);
    }

    public static JSFunctionData create(JSContext context, CallTarget callTarget, CallTarget constructTarget, CallTarget constructNewTarget, int length, String name, boolean isConstructor,
                    boolean isDerived, boolean isStrict, boolean isBuiltin, boolean needsParentFrame, boolean isGenerator, boolean isAsync, boolean isClassConstructor,
                    boolean strictFunctionProperties, boolean needsNewTarget, boolean isBound) {
        int flags = (isConstructor ? IS_CONSTRUCTOR : 0) | (isDerived ? IS_DERIVED : 0) | (isStrict ? IS_STRICT : 0) | (isBuiltin ? IS_BUILTIN : 0) |
                        (needsParentFrame ? NEEDS_PARENT_FRAME : 0) | (isGenerator ? IS_GENERATOR : 0) | (isAsync ? IS_ASYNC : 0) | (isClassConstructor ? IS_CLASS_CONSTRUCTOR : 0) |
                        (strictFunctionProperties ? STRICT_FUNCTION_PROPERTIES : 0) | (needsNewTarget ? NEEDS_NEW_TARGET : 0) | (isBound ? IS_BOUND : 0);
        return create(context, callTarget, constructTarget, constructNewTarget, length, name, flags);
    }

    public static JSFunctionData create(JSContext context, CallTarget callTarget, CallTarget constructTarget, int length, String name, boolean isConstructor, boolean isDerived, boolean strictMode,
                    boolean isBuiltin) {
        assert callTarget != null && constructTarget != null;
        return create(context, callTarget, constructTarget, constructTarget, length, name, isConstructor, isDerived, strictMode, isBuiltin, false, false, false, false, strictMode, false, false);
    }

    public static JSFunctionData createCallOnly(JSContext context, CallTarget callTarget, int length, String name) {
        assert callTarget != null;
        CallTarget constructTarget = context.getNotConstructibleCallTarget();
        return create(context, callTarget, constructTarget, constructTarget, length, name, false, false, false, true, false, false, false, false, false, false, false);
    }

    public static JSFunctionData create(JSContext context, int length, String name, boolean isConstructor, boolean isDerived, boolean strictMode, boolean isBuiltin) {
        return create(context, null, null, null, length, name, isConstructor, isDerived, strictMode, isBuiltin, false, false, false, false, strictMode, false, false);
    }

    public static JSFunctionData create(JSContext context, CallTarget callTarget, int length, String name) {
        assert callTarget != null;
        return create(context, callTarget, callTarget, callTarget, length, name, true, false, false, false, false, false, false, false, false, false, false);
    }

    public CallTarget getCallTarget() {
        CallTarget result = callTarget;
        if (result != null) {
            return result;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return ensureInitialized(Target.Call);
    }

    public CallTarget getConstructTarget() {
        CallTarget result = constructTarget;
        if (result != null) {
            return result;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return ensureInitialized(Target.Construct);
    }

    public CallTarget getConstructNewTarget() {
        CallTarget result = constructNewTarget;
        if (result != null) {
            return result;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return ensureInitialized(Target.ConstructNewTarget);
    }

    public JSContext getContext() {
        return context;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLength() {
        return length;
    }

    public boolean isConstructor() {
        return (flags & IS_CONSTRUCTOR) != 0;
    }

    public boolean isStrict() {
        return (flags & IS_STRICT) != 0;
    }

    public boolean hasStrictFunctionProperties() {
        return (flags & STRICT_FUNCTION_PROPERTIES) != 0;
    }

    public boolean isBuiltin() {
        return (flags & IS_BUILTIN) != 0;
    }

    public boolean needsParentFrame() {
        return (flags & NEEDS_PARENT_FRAME) != 0;
    }

    public boolean isGenerator() {
        return (flags & IS_GENERATOR) != 0;
    }

    public boolean isAsync() {
        return (flags & IS_ASYNC) != 0;
    }

    public boolean isAsyncGenerator() {
        return isGenerator() && isAsync();
    }

    public boolean isDerived() {
        return (flags & IS_DERIVED) != 0;
    }

    public boolean isClassConstructor() {
        return (flags & IS_CLASS_CONSTRUCTOR) != 0;
    }

    public boolean isPrototypeNotWritable() {
        return isClassConstructor();
    }

    public boolean requiresNew() {
        return isClassConstructor();
    }

    public boolean needsNewTarget() {
        return (flags & NEEDS_NEW_TARGET) != 0;
    }

    public boolean isBound() {
        return (flags & IS_BOUND) != 0;
    }

    public int getFlags() {
        return flags;
    }

    public CallTarget setCallTarget(CallTarget callTarget) {
        assert callTarget != null;
        if (UPDATER_CALL_TARGET.compareAndSet(this, null, callTarget)) {
            return callTarget;
        } else {
            return this.callTarget;
        }
    }

    public CallTarget setConstructTarget(CallTarget constructTarget) {
        assert constructTarget != null;
        if (UPDATER_CONSTRUCT_TARGET.compareAndSet(this, null, constructTarget)) {
            return constructTarget;
        } else {
            return this.constructTarget;
        }
    }

    public CallTarget setConstructNewTarget(CallTarget constructNewTarget) {
        assert constructNewTarget != null;
        if (UPDATER_CONSTRUCT_NEW_TARGET.compareAndSet(this, null, constructNewTarget)) {
            return constructNewTarget;
        } else {
            return this.constructNewTarget;
        }
    }

    public CallTarget setRootTarget(CallTarget rootTarget) {
        assert rootTarget != null;
        if (UPDATER_ROOT_TARGET.compareAndSet(this, null, rootTarget)) {
            return rootTarget;
        } else {
            return this.rootTarget;
        }
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "(" + name + ")";
    }

    public void setLazyInit(Initializer lazyInit) {
        assert this.lazyInit == null;
        this.lazyInit = lazyInit;
    }

    public boolean hasLazyInit() {
        return lazyInit != null;
    }

    private CallTarget ensureInitialized(Target target) {
        Initializer init = lazyInit;
        assert init != null;
        if (rootTarget == null) {
            init.initializeRoot(this);
            if (!(init instanceof CallTargetInitializer)) {
                lazyInit = init = (CallTargetInitializer) ((RootCallTarget) rootTarget).getRootNode();
            }
        }
        AtomicReferenceFieldUpdater<JSFunctionData, CallTarget> updater = target.getUpdater();
        CallTarget result = updater.get(this);
        if (result != null) {
            return result;
        }
        if (!(init instanceof CallTargetInitializer)) {
            init = (CallTargetInitializer) ((RootCallTarget) rootTarget).getRootNode();
        }
        ((CallTargetInitializer) init).initializeCallTarget(this, target, rootTarget);
        result = updater.get(this);
        assert result != null;
        return result;
    }

    public enum Target {
        Call(UPDATER_CALL_TARGET),
        Construct(UPDATER_CONSTRUCT_TARGET),
        ConstructNewTarget(UPDATER_CONSTRUCT_NEW_TARGET);

        private final AtomicReferenceFieldUpdater<JSFunctionData, CallTarget> updater;

        Target(AtomicReferenceFieldUpdater<JSFunctionData, CallTarget> updater) {
            this.updater = updater;
        }

        AtomicReferenceFieldUpdater<JSFunctionData, CallTarget> getUpdater() {
            return updater;
        }
    }

    public interface Initializer {
        void initializeRoot(JSFunctionData functionData);
    }

    public interface CallTargetInitializer extends Initializer {
        void initializeCallTarget(JSFunctionData functionData, Target target, CallTarget rootTarget);

        default void initializeEager(JSFunctionData functionData) {
            initializeRoot(functionData);
            CallTarget rootTarget = Objects.requireNonNull(functionData.rootTarget);
            initializeCallTarget(functionData, Target.Call, rootTarget);
            initializeCallTarget(functionData, Target.Construct, rootTarget);
            initializeCallTarget(functionData, Target.ConstructNewTarget, rootTarget);
        }
    }
}
