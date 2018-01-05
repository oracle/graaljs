/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Script

// Script::Compile

EXPORT_TO_JS(Compile) {
    Local<String> source = args[0].As<String>();
    Local<String> fileName = args[1].As<String>();

    Local<Script> script = Script::Compile(source, fileName);
    int id = script->GetUnboundScript()->GetId();
    args.GetReturnValue().Set(id);
}

EXPORT_TO_JS(CompileWithScriptOrigin) {
    Local<String> source = args[0].As<String>();
    Local<String> fileName = args[1].As<String>();

    ScriptOrigin* origin = new ScriptOrigin(fileName);

    Local<Script> script = Script::Compile(source, origin);
    int id = script->GetUnboundScript()->GetId();

    delete origin;

    args.GetReturnValue().Set(id);
}

// Script::Run

EXPORT_TO_JS(Run) {
    Local<String> source = args[0].As<String>();
    Local<String> fileName = args[1].As<String>();

    Local<Script> script = Script::Compile(source, fileName);
    Local<Value> result = script->Run();
    args.GetReturnValue().Set(result);
}

#undef SUITE
