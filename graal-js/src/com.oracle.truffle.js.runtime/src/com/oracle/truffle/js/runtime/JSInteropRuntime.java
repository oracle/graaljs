/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ForeignAccess;

public class JSInteropRuntime {
    private final ForeignAccess foreignAccess;
    private final ForeignAccess interopObjectForeignAccess;
    private final TruffleGlobalScopeImpl multilanguageGlobal;

    public JSInteropRuntime(ForeignAccess foreignAccess, ForeignAccess interopObjectForeignAccess, TruffleLanguage.Env env) {
        this.foreignAccess = foreignAccess;
        this.interopObjectForeignAccess = interopObjectForeignAccess;
        this.multilanguageGlobal = new TruffleGlobalScopeImpl(env);
    }

    public ForeignAccess getForeignAccessFactory() {
        return foreignAccess;
    }

    public ForeignAccess getInteropObjectForeignAccess() {
        return interopObjectForeignAccess;
    }

    public TruffleGlobalScopeImpl getMultilanguageGlobal() {
        return multilanguageGlobal;
    }
}
