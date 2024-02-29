/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.Set;

import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.JobCallback;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.PromiseReactionRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.UnmodifiableArrayList;

/**
 * Represents the import call expression syntax: {@code import(specifier)}.
 */
public class ImportCallNode extends JavaScriptNode {

    private static final HiddenKey CURRENT_MODULE_RECORD_KEY = new HiddenKey("%currentModuleRecord");
    private static final TruffleString ASSERT = Strings.constant("assert");

    @Child private JavaScriptNode argRefNode;
    private final ScriptOrModule activeScriptOrModule;
    @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
    @Child private JSToStringNode toStringNode;
    @Child private PromiseReactionJobNode promiseReactionJobNode;
    @Child private JavaScriptNode optionsRefNode;

    // lazily initialized
    @Child private JSFunctionCallNode callRejectNode;
    @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
    @Child private EnumerableOwnPropertyNamesNode enumerableOwnPropertyNamesNode;
    @Child private PropertyGetNode getWithNode;
    @Child private PropertyGetNode getAssertNode;

    private final JSContext context;

    protected ImportCallNode(JSContext context, JavaScriptNode argRefNode, ScriptOrModule activeScriptOrModule, JavaScriptNode optionsRefNode) {
        this.context = context;
        this.argRefNode = argRefNode;
        this.activeScriptOrModule = activeScriptOrModule;
        this.optionsRefNode = optionsRefNode;
        this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
        this.toStringNode = JSToStringNode.create();
        this.promiseReactionJobNode = PromiseReactionJobNode.create(context);
    }

    public static ImportCallNode create(JSContext context, JavaScriptNode argRefNode, ScriptOrModule activeScriptOrModule) {
        return new ImportCallNode(context, argRefNode, activeScriptOrModule, null);
    }

    public static ImportCallNode createWithOptions(JSContext context, JavaScriptNode specifierRefNode, ScriptOrModule activeScriptOrModule, JavaScriptNode optionsRefNode) {
        return new ImportCallNode(context, specifierRefNode, activeScriptOrModule, optionsRefNode);
    }

    @NeverDefault
    public static ImportCallNode create(JSContext context) {
        return create(context, null, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        ScriptOrModule referencingScriptOrModule = activeScriptOrModule;
        Object specifier = argRefNode.execute(frame);
        if (context.getLanguageOptions().importAttributes() && optionsRefNode != null) {
            return executeAttributes(frame, referencingScriptOrModule, specifier);
        } else {
            return executeWithoutAttributes(referencingScriptOrModule, specifier);
        }
    }

    private Object executeWithoutAttributes(ScriptOrModule referencingScriptOrModule, Object specifier) {
        PromiseCapabilityRecord promiseCapability = newPromiseCapability();
        TruffleString specifierString;
        try {
            specifierString = toStringNode.executeString(specifier);
        } catch (AbstractTruffleException ex) {
            return rejectPromise(promiseCapability, ex);
        }
        return hostImportModuleDynamically(referencingScriptOrModule, ModuleRequest.create(specifierString), promiseCapability);
    }

    @SuppressWarnings("unchecked")
    private Object executeAttributes(VirtualFrame frame, ScriptOrModule referencingScriptOrModule, Object specifier) {
        assert optionsRefNode != null;
        if (enumerableOwnPropertyNamesNode == null || getWithNode == null || getAssertNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            enumerableOwnPropertyNamesNode = insert(EnumerableOwnPropertyNamesNode.createKeys(context));
            getWithNode = insert(PropertyGetNode.create(Strings.WITH, context));
            getAssertNode = insert(PropertyGetNode.create(ASSERT, context));
        }
        Object options = optionsRefNode.execute(frame);
        PromiseCapabilityRecord promiseCapability = newPromiseCapability();
        TruffleString specifierString;
        try {
            specifierString = toStringNode.executeString(specifier);
        } catch (AbstractTruffleException ex) {
            return rejectPromise(promiseCapability, ex);
        }
        Map.Entry<TruffleString, TruffleString>[] attributes = null;
        if (options != Undefined.instance) {
            if (!JSRuntime.isObject(options)) {
                return rejectPromiseWithTypeError(promiseCapability, "The second argument to import() must be an object");
            }
            Object attributesObj;
            try {
                attributesObj = getWithNode.getValue(options);
                if (attributesObj == Undefined.instance) {
                    attributesObj = getAssertNode.getValue(options);
                }
            } catch (AbstractTruffleException ex) {
                return rejectPromise(promiseCapability, ex);
            }
            if (attributesObj != Undefined.instance) {
                if (!JSRuntime.isObject(attributesObj)) {
                    return rejectPromiseWithTypeError(promiseCapability, "The 'assert' option must be an object");
                }
                JSDynamicObject obj = (JSDynamicObject) attributesObj;
                UnmodifiableArrayList<? extends Object> keys;
                try {
                    keys = enumerableOwnPropertyNamesNode.execute(obj);
                } catch (AbstractTruffleException ex) {
                    return rejectPromise(promiseCapability, ex);
                }
                attributes = (Map.Entry<TruffleString, TruffleString>[]) new Map.Entry<?, ?>[keys.size()];
                boolean allStrings = true;
                for (int i = 0; i < keys.size(); i++) {
                    TruffleString key = (TruffleString) keys.get(i);
                    Object value;
                    try {
                        value = JSObject.get(obj, key);
                    } catch (AbstractTruffleException ex) {
                        return rejectPromise(promiseCapability, ex);
                    }
                    if (value instanceof TruffleString valueStr) {
                        attributes[i] = Boundaries.mapEntry(key, valueStr);
                    } else {
                        // Read all values before rejecting the promise,
                        // we were supposed to do EnumerableOwnProperties(KEY+VALUE) above.
                        allStrings = false;
                    }
                }
                if (!allStrings) {
                    return rejectPromiseWithTypeError(promiseCapability, "Import assertion value must be a string");
                }
            }
        }
        ModuleRequest moduleRequest = attributes == null ? ModuleRequest.create(specifierString) : createModuleRequestWithAttributes(specifierString, attributes);
        return hostImportModuleDynamically(referencingScriptOrModule, moduleRequest, promiseCapability);
    }

    @TruffleBoundary
    private static ModuleRequest createModuleRequestWithAttributes(TruffleString specifierString, Map.Entry<TruffleString, TruffleString>[] attributes) {
        return ModuleRequest.create(specifierString, attributes);
    }

    public final JSDynamicObject hostImportModuleDynamically(ScriptOrModule referencingScriptOrModule, ModuleRequest moduleRequest, PromiseCapabilityRecord promiseCapability) {
        JSRealm realm = getRealm();
        if (context.hasImportModuleDynamicallyCallbackBeenSet()) {
            JSDynamicObject promise = context.hostImportModuleDynamically(realm, referencingScriptOrModule, moduleRequest);
            if (promise == null) {
                return rejectPromise(promiseCapability, createTypeErrorCannotImport(moduleRequest.getSpecifier()));
            }
            assert JSPromise.isJSPromise(promise);
            return promise;
        } else {
            context.enqueuePromiseJob(realm, createImportModuleDynamicallyJob(referencingScriptOrModule, moduleRequest, promiseCapability, realm));
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

    private JSDynamicObject rejectPromise(PromiseCapabilityRecord promiseCapability, AbstractTruffleException ex) {
        if (callRejectNode == null || getErrorObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callRejectNode = insert(JSFunctionCallNode.createCall());
            getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
        }
        Object error = getErrorObjectNode.execute(ex);
        callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
        return promiseCapability.getPromise();
    }

    private Object rejectPromiseWithTypeError(PromiseCapabilityRecord promiseCapability, String errorMessage) {
        if (callRejectNode == null) {
            // Just to cut off before createTypeError. Nodes are initialized in rejectPromise().
            CompilerDirectives.transferToInterpreterAndInvalidate();
        }
        return rejectPromise(promiseCapability, Errors.createTypeError(errorMessage, this));
    }

    @TruffleBoundary
    private static JSException createTypeErrorCannotImport(TruffleString specifier) {
        return Errors.createError("Cannot dynamically import module: " + specifier);
    }

    /**
     * Arguments to HostLoadImportedModule.
     */
    private record LoadImportedModuleRequest(
                    ScriptOrModule referencingScriptOrModule,
                    ModuleRequest moduleRequest,
                    PromiseCapabilityRecord promiseCapability) {
    }

    /**
     * Returns a promise job that performs both HostImportModuleDynamically and FinishDynamicImport.
     */
    public JSFunctionObject createImportModuleDynamicallyJob(ScriptOrModule referencingScriptOrModule, ModuleRequest moduleRequest, PromiseCapabilityRecord promiseCapability, JSRealm realm) {
        JobCallback importModuleDynamicallyHandler = realm.getAgent().hostMakeJobCallback(createImportModuleDynamicallyHandler(realm));
        if (context.isOptionTopLevelAwait()) {
            LoadImportedModuleRequest request = new LoadImportedModuleRequest(referencingScriptOrModule, moduleRequest, promiseCapability);
            return promiseReactionJobNode.execute(PromiseReactionRecord.create(null, importModuleDynamicallyHandler, true), request);
        } else {
            LoadImportedModuleRequest request = new LoadImportedModuleRequest(referencingScriptOrModule, moduleRequest, null);
            return promiseReactionJobNode.execute(PromiseReactionRecord.create(promiseCapability, importModuleDynamicallyHandler, true), request);
        }
    }

    /**
     * Returns a handler function to be used together with a PromiseReactionJob in order to perform
     * the steps of both HostImportModuleDynamically and FinishDynamicImport.
     */
    private JSFunctionObject createImportModuleDynamicallyHandler(JSRealm realm) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.ImportModuleDynamically, (c) -> createImportModuleDynamicallyHandlerImpl(c));
        return JSFunction.create(realm, functionData);
    }

    private static JSFunctionData createImportModuleDynamicallyHandlerImpl(JSContext context) {
        class ImportModuleDynamicallyRootNode extends JavaScriptRealmBoundaryRootNode implements AsyncHandlerRootNode {
            @Child protected JavaScriptNode argumentNode = AccessIndexedArgumentNode.create(0);

            protected ImportModuleDynamicallyRootNode(JavaScriptLanguage lang) {
                super(lang);
            }

            @Override
            public Object executeInRealm(VirtualFrame frame) {
                LoadImportedModuleRequest request = (LoadImportedModuleRequest) argumentNode.execute(frame);
                ScriptOrModule referencingScriptOrModule = request.referencingScriptOrModule();
                ModuleRequest moduleRequest = request.moduleRequest();
                JSModuleRecord moduleRecord = context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, moduleRequest);
                return finishDynamicImport(getRealm(), moduleRecord, referencingScriptOrModule, moduleRequest);
            }

            protected Object finishDynamicImport(JSRealm realm, JSModuleRecord moduleRecord, ScriptOrModule referencingScriptOrModule, ModuleRequest moduleRequest) {
                context.getEvaluator().moduleLinking(realm, moduleRecord);
                context.getEvaluator().moduleEvaluation(realm, moduleRecord);
                if (moduleRecord.getEvaluationError() != null) {
                    throw JSRuntime.rethrow(moduleRecord.getEvaluationError());
                }
                // Note: PromiseReactionJob performs the promise rejection and resolution.
                assert moduleRecord == context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, moduleRequest);
                // Evaluate has already been invoked on moduleRecord and successfully completed.
                assert moduleRecord.hasBeenEvaluated();
                return context.getEvaluator().getModuleNamespace(moduleRecord);
            }

            @Override
            public AsyncStackTraceInfo getAsyncStackTraceInfo(JSFunctionObject handlerFunction, Object argument) {
                if (argument instanceof LoadImportedModuleRequest request) {
                    return new AsyncStackTraceInfo(request.promiseCapability().getPromise(), null);
                }
                return new AsyncStackTraceInfo();
            }
        }

        class TopLevelAwaitImportModuleDynamicallyRootNode extends ImportModuleDynamicallyRootNode {
            @Child private PerformPromiseThenNode promiseThenNode = PerformPromiseThenNode.create(context);
            @Child private JSFunctionCallNode callPromiseResolve = JSFunctionCallNode.createCall();
            @Child private JSFunctionCallNode callPromiseReject;
            @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
            @Child private PropertySetNode setModuleRecord;

            protected TopLevelAwaitImportModuleDynamicallyRootNode(JavaScriptLanguage lang) {
                super(lang);
            }

            @Override
            public Object executeInRealm(VirtualFrame frame) {
                LoadImportedModuleRequest request = (LoadImportedModuleRequest) argumentNode.execute(frame);
                ScriptOrModule referencingScriptOrModule = request.referencingScriptOrModule();
                ModuleRequest moduleRequest = request.moduleRequest();
                PromiseCapabilityRecord importPromiseCapability = request.promiseCapability();
                try {
                    JSRealm realm = getRealm();
                    assert realm == JSFunction.getRealm(JSFrameUtil.getFunctionObject(frame));
                    JSModuleRecord moduleRecord = context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, moduleRequest);
                    if (moduleRecord.hasTLA()) {
                        context.getEvaluator().moduleLinking(realm, moduleRecord);
                        JSPromiseObject evaluatePromise = (JSPromiseObject) context.getEvaluator().moduleEvaluation(realm, moduleRecord);
                        JSDynamicObject resolve = createFinishDynamicImportCapabilityCallback(context, realm, moduleRecord, false);
                        JSDynamicObject reject = createFinishDynamicImportCapabilityCallback(context, realm, moduleRecord, true);
                        promiseThenNode.execute(evaluatePromise, resolve, reject, importPromiseCapability);
                    } else {
                        Object result = finishDynamicImport(realm, moduleRecord, referencingScriptOrModule, moduleRequest);
                        if (moduleRecord.isAsyncEvaluation()) {
                            // Some module import started an async loading chain. The top-level
                            // capability will reject/resolve the dynamic import promise.
                            JSPromiseObject evaluatePromise = (JSPromiseObject) moduleRecord.getTopLevelCapability().getPromise();
                            promiseThenNode.execute(evaluatePromise, importPromiseCapability.getResolve(), importPromiseCapability.getReject(), null);
                        } else {
                            callPromiseResolve.executeCall(JSArguments.create(Undefined.instance, importPromiseCapability.getResolve(), result));
                        }
                    }
                } catch (AbstractTruffleException ex) {
                    rejectPromise(importPromiseCapability, ex);
                }
                return Undefined.instance;
            }

            private void rejectPromise(PromiseCapabilityRecord moduleLoadedCapability, AbstractTruffleException ex) {
                if (getErrorObjectNode == null || callPromiseReject == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                    callPromiseReject = insert(JSFunctionCallNode.createCall());
                }
                Object errorObject = getErrorObjectNode.execute(ex);
                callPromiseReject.executeCall(JSArguments.create(Undefined.instance, moduleLoadedCapability.getReject(), errorObject));
            }

            private JSDynamicObject createFinishDynamicImportCapabilityCallback(JSContext cx, JSRealm realm, JSModuleRecord moduleRecord, boolean onReject) {
                JSFunctionData functionData;
                if (onReject) {
                    functionData = cx.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.FinishImportModuleDynamicallyReject, (c) -> createFinishDynamicImportNormalImpl(c, true));
                } else {
                    functionData = cx.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.FinishImportModuleDynamicallyResolve, (c) -> createFinishDynamicImportNormalImpl(c, false));
                }
                JSDynamicObject resolveFunction = JSFunction.create(realm, functionData);
                if (setModuleRecord == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setModuleRecord = insert(PropertySetNode.createSetHidden(CURRENT_MODULE_RECORD_KEY, cx));
                }
                setModuleRecord.setValue(resolveFunction, moduleRecord);
                return resolveFunction;
            }
        }

        JavaScriptRootNode root = context.isOptionTopLevelAwait()
                        ? new TopLevelAwaitImportModuleDynamicallyRootNode(context.getLanguage())
                        : new ImportModuleDynamicallyRootNode(context.getLanguage());
        return JSFunctionData.createCallOnly(context, root.getCallTarget(), 0, Strings.EMPTY_STRING);
    }

    private static JSFunctionData createFinishDynamicImportNormalImpl(JSContext cx, boolean onReject) {
        class FinishDynamicImportNormalRootNode extends JavaScriptRootNode {
            @Child private PropertyGetNode getModuleRecord = PropertyGetNode.createGetHidden(CURRENT_MODULE_RECORD_KEY, cx);

            @Override
            public Object execute(VirtualFrame frame) {
                // ECMA 16.2.1.9 FinishDynamicImport(): reject/resolve `innerPromise`.
                // Promise reaction will be handled by the promise registered via `then`.
                JSDynamicObject thisFunction = (JSDynamicObject) JSArguments.getFunctionObject(frame.getArguments());
                JSModuleRecord moduleRecord = (JSModuleRecord) getModuleRecord.getValue(thisFunction);
                assert moduleRecord != null;
                if (onReject) {
                    assert moduleRecord.getEvaluationError() != null;
                    throw JSRuntime.rethrow(moduleRecord.getEvaluationError());
                } else {
                    return cx.getEvaluator().getModuleNamespace(moduleRecord);
                }
            }
        }
        return JSFunctionData.createCallOnly(cx, new FinishDynamicImportNormalRootNode().getCallTarget(), 0, Strings.EMPTY_STRING);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        if (optionsRefNode == null) {
            return ImportCallNode.create(context, cloneUninitialized(argRefNode, materializedTags), activeScriptOrModule);
        } else {
            return ImportCallNode.createWithOptions(context, cloneUninitialized(argRefNode, materializedTags), activeScriptOrModule,
                            cloneUninitialized(optionsRefNode, materializedTags));
        }
    }
}
