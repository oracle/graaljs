/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

(function arrayBufferArgumentsAreLengths() {
    var length = new ArrayBuffer(4);
    assertSame(0, new ArrayBuffer(length).byteLength);

    var resizable = new ArrayBuffer(length, { maxByteLength: 8 });
    assertSame(0, resizable.byteLength);
    assertSame(8, resizable.maxByteLength);
})();

(function arrayBufferConstructorOrder() {
    var log = [];
    var length = new ArrayBuffer(4);
    length.valueOf = function() {
        log.push("length");
        return 2;
    };
    var options = {
        get maxByteLength() {
            log.push("max");
            return 8;
        }
    };
    var newTarget = new Proxy(ArrayBuffer, {
        get(target, key, receiver) {
            if (key === "prototype") {
                log.push("proto");
            }
            return Reflect.get(target, key, receiver);
        }
    });

    var buffer = Reflect.construct(ArrayBuffer, [length, options], newTarget);
    assertSameContent(["length", "max", "proto"], log);
    assertSame(2, buffer.byteLength);
    assertSame(8, buffer.maxByteLength);
})();

(function sharedArrayBufferArgumentsAreLengths() {
    assertSame(0, new ArrayBuffer(new SharedArrayBuffer(4)).byteLength);
})();
