/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;

/**
 * Source Text Module Record.
 */
public final class JSModuleRecord {
    private final Object module;
    private final JSContext context;
    private final JSModuleLoader moduleLoader;
    private final Source source;

    /** Resolved is true after ModuleDeclarationInstantiation(). */
    private boolean resolved;
    /** Evaluated is true after ModuleEvaluation(). */
    private boolean evaluated;

    private JSFunctionData functionData;

    /** Lazily initialized Module Namespace object ({@code [[Namespace]]}). */
    private DynamicObject namespace;
    /** Lazily initialized frame ({@code [[Environment]]}). */
    private MaterializedFrame environment;

    private Runnable finishTranslation;

    public JSModuleRecord(Object module, JSContext context, JSModuleLoader moduleLoader, Source source, Runnable finishTranslation) {
        this.module = module;
        this.context = context;
        this.moduleLoader = moduleLoader;
        this.source = source;
        this.finishTranslation = finishTranslation;
    }

    public Object getModule() {
        return module;
    }

    public JSContext getContext() {
        return context;
    }

    public JSModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    public Source getSource() {
        return source;
    }

    public JSFunctionData getFunctionData() {
        assert functionData != null;
        return functionData;
    }

    public void setFunctionData(JSFunctionData functionData) {
        assert this.functionData == null;
        this.functionData = functionData;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public DynamicObject getNamespace() {
        return namespace;
    }

    public void setNamespace(DynamicObject namespace) {
        assert this.namespace == null;
        this.namespace = namespace;
    }

    public MaterializedFrame getEnvironment() {
        return environment;
    }

    public void setEnvironment(MaterializedFrame environment) {
        assert this.environment == null;
        this.environment = environment;
    }

    public void finishTranslation() {
        assert isResolved();
        finishTranslation.run();
        finishTranslation = null;
    }
}
