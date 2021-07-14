/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.promise;

import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.PromiseReactionRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.Triple;
import com.oracle.truffle.js.runtime.util.UnmodifiableArrayList;

import java.util.Map;
import java.util.Set;

/**
 * Represents the import call expression syntax: {@code import(specifier)}.
 */
public class ImportCallNode extends JavaScriptNode {
    private static final String ASSERTIONS = "assert";

    @Child private JavaScriptNode argRefNode;
    @Child private JavaScriptNode activeScriptOrModuleNode;
    @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
    @Child private JSToStringNode toStringNode;
    @Child private PromiseReactionJobNode promiseReactionJobNode;
    @Child private JavaScriptNode optionsRefNode;

    // lazily initialized
    @Child private JSFunctionCallNode callRejectNode;
    @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
    @Child private InteropLibrary exceptions;
    @Child private EnumerableOwnPropertyNamesNode enumerableOwnPropertyNamesNode;
    @Child private PropertyGetNode getAssertionsNode;

    private final JSContext context;

    protected ImportCallNode(JSContext context, JavaScriptNode argRefNode, JavaScriptNode activeScriptOrModuleNode, JavaScriptNode optionsRefNode) {
        this.context = context;
        this.argRefNode = argRefNode;
        this.activeScriptOrModuleNode = activeScriptOrModuleNode;
        this.optionsRefNode = optionsRefNode;
        this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
        this.toStringNode = JSToStringNode.create();
        this.promiseReactionJobNode = PromiseReactionJobNode.create(context);
    }

    public static ImportCallNode create(JSContext context, JavaScriptNode argRefNode, JavaScriptNode activeScriptOrModuleNode) {
        return new ImportCallNode(context, argRefNode, activeScriptOrModuleNode, null);
    }

    public static ImportCallNode createWithOptions(JSContext context, JavaScriptNode specifierRefNode, JavaScriptNode activeScriptOrModuleNode, JavaScriptNode optionsRefNode) {
        return new ImportCallNode(context, specifierRefNode, activeScriptOrModuleNode, optionsRefNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object referencingScriptOrModule = getActiveScriptOrModule(frame);
        Object specifier = argRefNode.execute(frame);
        if (context.getContextOptions().isImportAssertions() && optionsRefNode != null) {
            return executeAssertions(frame, referencingScriptOrModule, specifier);
        } else {
            return executeWithoutAssertions(referencingScriptOrModule, specifier);
        }
    }

    private Object executeWithoutAssertions(Object referencingScriptOrModule, Object specifier) {
        String specifierString;
        try {
            specifierString = toStringNode.executeString(specifier);
        } catch (Throwable ex) {
            if (TryCatchNode.shouldCatch(ex, exceptions())) {
                return newRejectedPromiseFromException(ex);
            } else {
                throw ex;
            }
        }
        return hostImportModuleDynamically(referencingScriptOrModule, ModuleRequest.create(specifierString), newPromiseCapability());
    }

    private Object executeAssertions(VirtualFrame frame, Object referencingScriptOrModule, Object specifier) {
        assert optionsRefNode != null;
        if (enumerableOwnPropertyNamesNode == null || getAssertionsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            enumerableOwnPropertyNamesNode = insert(EnumerableOwnPropertyNamesNode.createKeys(context));
            getAssertionsNode = insert(PropertyGetNode.create(ASSERTIONS, context));
        }
        Object options = optionsRefNode.execute(frame);
        PromiseCapabilityRecord promiseCapability = newPromiseCapability();
        String specifierString;
        try {
            specifierString = toStringNode.executeString(specifier);
        } catch (Throwable ex) {
            if (TryCatchNode.shouldCatch(ex, exceptions())) {
                return newRejectedPromiseFromException(ex);
            } else {
                throw ex;
            }
        }
        Map<String, String> assertions = Boundaries.hashMapCreate();
        if (options != Undefined.instance) {
            if (!JSRuntime.isObject(options)) {
                return rejectPromise(promiseCapability, "The second argument to import() must be an object");
            }
            Object assertionsObj;
            try {
                assertionsObj = getAssertionsNode.getValue(options);
            } catch (Throwable ex) {
                if (TryCatchNode.shouldCatch(ex, exceptions())) {
                    return newRejectedPromiseFromException(ex);
                } else {
                    throw ex;
                }
            }
            if (assertionsObj != Undefined.instance) {
                if (!JSRuntime.isObject(assertionsObj)) {
                    return rejectPromise(promiseCapability, "The 'assert' option must be an object");
                }
                DynamicObject obj = (DynamicObject) assertionsObj;
                UnmodifiableArrayList<? extends Object> keys;
                try {
                    keys = enumerableOwnPropertyNamesNode.execute(obj);
                } catch (Throwable ex) {
                    if (TryCatchNode.shouldCatch(ex, exceptions())) {
                        return newRejectedPromiseFromException(ex);
                    } else {
                        throw ex;
                    }
                }
                for (int i = 0; i < keys.size(); i++) {
                    String key;
                    Object value;
                    try {
                        key = (String) keys.get(i);
                        value = JSObject.get(obj, key);
                    } catch (Throwable ex) {
                        if (TryCatchNode.shouldCatch(ex, exceptions())) {
                            return newRejectedPromiseFromException(ex);
                        } else {
                            throw ex;
                        }
                    }
                    if (!JSRuntime.isString(value)) {
                        return rejectPromise(promiseCapability, "Import assertion value must be a string");
                    }
                    Boundaries.mapPut(assertions, key, JSRuntime.toStringIsString(value));
                }
            }
        }
        return hostImportModuleDynamically(referencingScriptOrModule, ModuleRequest.create(specifierString, assertions), promiseCapability);
    }

    private Object rejectPromise(PromiseCapabilityRecord promiseCapability, String errorMessage) {
        if (callRejectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callRejectNode = insert(JSFunctionCallNode.createCall());
        }
        callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), Errors.createTypeError(errorMessage, this)));
        return promiseCapability.getPromise();
    }

    private Object getActiveScriptOrModule(VirtualFrame frame) {
        if (activeScriptOrModuleNode != null) {
            return activeScriptOrModuleNode.execute(frame);
        } else {
            return new ScriptOrModule(context, getEncapsulatingSourceSection().getSource());
        }
    }

    private DynamicObject hostImportModuleDynamically(Object referencingScriptOrModule, ModuleRequest moduleRequest, PromiseCapabilityRecord promiseCapability) {
        JSRealm realm = context.getRealm();
        if (context.hasImportModuleDynamicallyCallbackBeenSet()) {
            DynamicObject promise = context.hostImportModuleDynamically(realm, (ScriptOrModule) referencingScriptOrModule, moduleRequest);
            if (promise == null) {
                return newRejectedPromiseFromException(createTypeErrorCannotImport(moduleRequest.getSpecifier()));
            }
            assert JSPromise.isJSPromise(promise);
            return promise;
        } else {
            context.promiseEnqueueJob(realm, createImportModuleDynamicallyJob((ScriptOrModule) referencingScriptOrModule, moduleRequest, promiseCapability));
            return promiseCapability.getPromise();
        }
    }

    private PromiseCapabilityRecord newPromiseCapability() {
        if (newPromiseCapabilityNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            newPromiseCapabilityNode = insert(NewPromiseCapabilityNode.create(context));
        }
        return newPromiseCapabilityNode.executeDefault();
    }

    private DynamicObject newRejectedPromiseFromException(Throwable ex) {
        if (callRejectNode == null || getErrorObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callRejectNode = insert(JSFunctionCallNode.createCall());
            getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
        }
        PromiseCapabilityRecord promiseCapability = newPromiseCapability();
        callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), getErrorObjectNode.execute(ex)));
        return promiseCapability.getPromise();
    }

    private InteropLibrary exceptions() {
        InteropLibrary e = exceptions;
        if (e == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            exceptions = e = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
        }
        return e;
    }

    @TruffleBoundary
    private static JSException createTypeErrorCannotImport(String specifier) {
        return Errors.createError("Cannot dynamically import module: " + specifier);
    }

    /**
     * Returns a promise job that performs both HostImportModuleDynamically and FinishDynamicImport.
     */
    public DynamicObject createImportModuleDynamicallyJob(ScriptOrModule referencingScriptOrModule, ModuleRequest moduleRequest, PromiseCapabilityRecord promiseCapability) {
        if (context.isOptionTopLevelAwait()) {
            Triple<ScriptOrModule, ModuleRequest, PromiseCapabilityRecord> request = new Triple<>(referencingScriptOrModule, moduleRequest, promiseCapability);
            PromiseCapabilityRecord startModuleLoadCapability = newPromiseCapability();
            PromiseReactionRecord startModuleLoad = PromiseReactionRecord.create(startModuleLoadCapability, createImportModuleDynamicallyHandler(), true);
            return promiseReactionJobNode.execute(startModuleLoad, request);
        } else {
            Pair<ScriptOrModule, ModuleRequest> request = new Pair<>(referencingScriptOrModule, moduleRequest);
            return promiseReactionJobNode.execute(PromiseReactionRecord.create(promiseCapability, createImportModuleDynamicallyHandler(), true), request);
        }
    }

    /**
     * Returns a handler function to be used together with a PromiseReactionJob in order to perform
     * the steps of both HostImportModuleDynamically and FinishDynamicImport.
     */
    private DynamicObject createImportModuleDynamicallyHandler() {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.ImportModuleDynamically, (c) -> createImportModuleDynamicallyHandlerImpl(c));
        return JSFunction.create(context.getRealm(), functionData);
    }

    private static JSFunctionData createImportModuleDynamicallyHandlerImpl(JSContext context) {
        class ImportModuleDynamicallyRootNode extends JavaScriptRootNode {
            @Child protected JavaScriptNode argumentNode = AccessIndexedArgumentNode.create(0);

            @SuppressWarnings("unchecked")
            @Override
            public Object execute(VirtualFrame frame) {
                Pair<ScriptOrModule, ModuleRequest> request = (Pair<ScriptOrModule, ModuleRequest>) argumentNode.execute(frame);
                ScriptOrModule referencingScriptOrModule = request.getFirst();
                ModuleRequest moduleRequest = request.getSecond();
                JSModuleRecord moduleRecord = context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, moduleRequest);
                return finishDynamicImport(context.getRealm(), moduleRecord, referencingScriptOrModule, moduleRequest);
            }

            protected Object finishDynamicImport(JSRealm realm, JSModuleRecord moduleRecord, ScriptOrModule referencingScriptOrModule, ModuleRequest moduleRequest) {
                context.getEvaluator().moduleInstantiation(realm, moduleRecord);
                context.getEvaluator().moduleEvaluation(realm, moduleRecord);
                if (moduleRecord.getEvaluationError() != null) {
                    throw JSRuntime.rethrow(moduleRecord.getEvaluationError());
                }
                // Note: PromiseReactionJob performs the promise rejection and resolution.
                assert moduleRecord == context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, moduleRequest);
                // Evaluate has already been invoked on moduleRecord and successfully completed.
                assert moduleRecord.isEvaluated();
                return context.getEvaluator().getModuleNamespace(moduleRecord);
            }
        }

        class TopLevelAwaitImportModuleDynamicallyRootNode extends ImportModuleDynamicallyRootNode {
            @Child private PerformPromiseThenNode promiseThenNode = PerformPromiseThenNode.create(context);
            @Child private JSFunctionCallNode callPromiseReaction = JSFunctionCallNode.createCall();
            @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
            @Child private InteropLibrary exceptions;

            @SuppressWarnings("unchecked")
            @Override
            public Object execute(VirtualFrame frame) {
                Triple<ScriptOrModule, ModuleRequest, PromiseCapabilityRecord> request = (Triple<ScriptOrModule, ModuleRequest, PromiseCapabilityRecord>) argumentNode.execute(frame);
                ScriptOrModule referencingScriptOrModule = request.getFirst();
                ModuleRequest moduleRequest = request.getSecond();
                PromiseCapabilityRecord moduleLoadedCapability = request.getThird();
                try {
                    JSModuleRecord moduleRecord = context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, moduleRequest);
                    JSRealm realm = context.getRealm();
                    if (moduleRecord.isTopLevelAsync()) {
                        context.getEvaluator().moduleInstantiation(realm, moduleRecord);
                        Object moduleLoadedStartPromise = context.getEvaluator().moduleEvaluation(realm, moduleRecord);
                        assert JSPromise.isJSPromise(moduleLoadedStartPromise);
                        promiseThenNode.execute((DynamicObject) moduleLoadedStartPromise, moduleLoadedCapability.getResolve(), moduleLoadedCapability.getReject(), moduleLoadedCapability);
                    } else {
                        Object result = finishDynamicImport(realm, moduleRecord, referencingScriptOrModule, moduleRequest);
                        if (moduleRecord.isAsyncEvaluating()) {
                            // Some module import started an async loading chain. The top-level
                            // capability will reject/resolve the dynamic import promise.
                            PromiseCapabilityRecord topLevelCapability = moduleRecord.getTopLevelCapability();
                            promiseThenNode.execute(topLevelCapability.getPromise(), moduleLoadedCapability.getResolve(), moduleLoadedCapability.getReject(), null);
                        } else {
                            callPromiseReaction.executeCall(JSArguments.create(Undefined.instance, moduleLoadedCapability.getResolve(), result));
                        }
                    }
                } catch (Throwable t) {
                    if (shouldCatch(t)) {
                        Object errorObject = getErrorObjectNode.execute(t);
                        callPromiseReaction.executeCall(JSArguments.create(Undefined.instance, moduleLoadedCapability.getReject(), errorObject));
                    } else {
                        throw t;
                    }
                }
                return Undefined.instance;
            }

            private boolean shouldCatch(Throwable exception) {
                if (getErrorObjectNode == null || exceptions == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                    exceptions = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                }
                return TryCatchNode.shouldCatch(exception, exceptions);
            }
        }

        JavaScriptRootNode root = context.isOptionTopLevelAwait() ? new TopLevelAwaitImportModuleDynamicallyRootNode() : new ImportModuleDynamicallyRootNode();
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(root);
        return JSFunctionData.createCallOnly(context, callTarget, 0, "");
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        if (optionsRefNode == null) {
            return ImportCallNode.create(context, cloneUninitialized(argRefNode, materializedTags), cloneUninitialized(activeScriptOrModuleNode, materializedTags));
        } else {
            return ImportCallNode.createWithOptions(context, cloneUninitialized(argRefNode, materializedTags), cloneUninitialized(activeScriptOrModuleNode, materializedTags),
                            cloneUninitialized(optionsRefNode, materializedTags));
        }
    }
}
