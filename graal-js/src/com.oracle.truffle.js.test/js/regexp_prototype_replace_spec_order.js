/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

let accessLog = [];

for (const re of [new RegExp("()()()()()()()()?(1)", "g")]) {
    let execCounter = 0;
    re.exec = function exec_(...args) {
        const nExec = execCounter++;
        const m = RegExp.prototype.exec.apply(this, args);
        if (m === null) {
            return null;
        }
        return new Proxy(m, {
            get(target, p, receiver) {
                let value = Reflect.get(target, p, receiver);
                accessLog.push(nExec + ": result" + (p >= 0 ? "[" + p + "]" : "." + p));
                return value;
            },
            defineProperty(target, p, desc) {
                throw new Error("unexpected defineProperty: " + p);
            },
            set(target, p, value, receiver) {
                throw new Error("unexpected set: " + p);
            },
        });
    }
    assertSame("<1><1>", "11".replace(re, "<$9>"));
}

assertSame(accessLog.join("\n"), `
0: result[0]
1: result[0]
0: result.length
0: result[0]
0: result.index
0: result[1]
0: result[2]
0: result[3]
0: result[4]
0: result[5]
0: result[6]
0: result[7]
0: result[8]
0: result[9]
0: result.groups
1: result.length
1: result[0]
1: result.index
1: result[1]
1: result[2]
1: result[3]
1: result[4]
1: result[5]
1: result[6]
1: result[7]
1: result[8]
1: result[9]
1: result.groups
`.trim());
