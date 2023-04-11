/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests dynamic import via indirect eval and dynamic function.
 *
 * @option unhandled-rejections=throw
 */

var importedUrl = './GR-38391import.mjs';


async function basic() {
    await import(importedUrl);
}
async function directEval() {
    await eval("import(importedUrl)");
}
async function indirectEval() {
    await (0,eval)("import(importedUrl)");
}
async function dynamicFunction() {
    await (new Function("return import(importedUrl)"))();
}
[basic, directEval, indirectEval, dynamicFunction].reduce((p, f) => p.then(() => f().catch(err => {
        console.error(`${f.name}() => ${err}`);
        throw err;
    })), Promise.resolve());
