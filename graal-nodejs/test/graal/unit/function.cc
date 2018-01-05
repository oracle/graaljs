/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Function

// Function::NewInstance

EXPORT_TO_JS(NewInstance) {
    Local<Function> func = args[0].As<Function>();
    args.GetReturnValue().Set(func->NewInstance());
}

EXPORT_TO_JS(NewInstanceWithArguments) {
    int argc = args[0].As<Number>()->Value();
    Local<Value>* argv = 0;
    printf("%d\n", args.Length());

    Local<Function> func = args[0].As<Function>();
    args.GetReturnValue().Set(func->NewInstance(argc, argv));
}

// Function::SetName

EXPORT_TO_JS(SetName) {
    Local<Function> func = args[0].As<Function>();
    func->SetName(args[1].As<String>());
    args.GetReturnValue().Set(func);
}

// Function::Call

EXPORT_TO_JS(Call) {
    Local<Function> func = args[0].As<Function>();
    Local<Object> recv = args[1].As<Object>();
    int argc = args[2].As<Integer>()->Value();

    Local<Value>* argv = new Local<Value>[argc];
    for (int i = 0; i < argc; i++) {
        argv[i] = args[i + 3];
    }
    args.GetReturnValue().Set(func->Call(recv, argc, argv));
    delete[] argv;
}

#undef SUITE
