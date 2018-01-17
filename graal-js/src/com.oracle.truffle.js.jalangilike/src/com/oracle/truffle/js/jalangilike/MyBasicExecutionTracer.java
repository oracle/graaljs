package com.oracle.truffle.js.jalangilike;

import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;

@Registration(id = MyBasicExecutionTracer.ID, services = {MyBasicExecutionTracer.class})
public class MyBasicExecutionTracer extends TruffleInstrument {

    public static final String ID = "MyBasicExecutionTracer";

    public Env environment;

    @Override
    protected void onCreate(Env env) {
        this.environment = env;
        env.registerService(this);
    }

}
