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

#define SUITE Cast

// Array::Cast

EXPORT_TO_JS(Array) {
    args.GetReturnValue().Set(Local<Array>::Cast(args[0]));
}

// Date::Cast

EXPORT_TO_JS(Date) {
    args.GetReturnValue().Set(Local<Date>::Cast(args[0]));
}

// Function::Cast

EXPORT_TO_JS(Function) {
    args.GetReturnValue().Set(Local<Function>::Cast(args[0]));
}

// Integer::Cast

EXPORT_TO_JS(Integer) {
    args.GetReturnValue().Set(Local<Integer>::Cast(args[0]));
}

// Number::Cast

EXPORT_TO_JS(Number) {
    args.GetReturnValue().Set(Local<Number>::Cast(args[0]));
}

// Object::Cast

EXPORT_TO_JS(Object) {
    args.GetReturnValue().Set(Local<Object>::Cast(args[0]));
}

// RegExp::Cast

EXPORT_TO_JS(RegExp) {
    args.GetReturnValue().Set(Local<RegExp>::Cast(args[0]));
}

// String::Cast

EXPORT_TO_JS(String) {
    args.GetReturnValue().Set(Local<String>::Cast(args[0]));
}

// Symbol::Cast

EXPORT_TO_JS(Symbol) {
    args.GetReturnValue().Set(Local<Symbol>::Cast(args[0]));
}

#undef SUITE
