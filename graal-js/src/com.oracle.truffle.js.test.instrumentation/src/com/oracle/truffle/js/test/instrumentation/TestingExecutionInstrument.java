/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;

/**
 * Instrument used by the tests to trace runtime events.
 */
@Registration(id = TestingExecutionInstrument.ID, services = {TestingExecutionInstrument.class})
public class TestingExecutionInstrument extends TruffleInstrument {

    public static final String ID = "TestingExecutionInstrument";

    public Env environment;

    @Override
    protected void onCreate(Env env) {
        this.environment = env;
        env.registerService(this);
    }

}
