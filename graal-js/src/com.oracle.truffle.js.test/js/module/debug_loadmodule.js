/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

"use strict";

load("../assert.js");

class ModuleLoader {
  constructor(modules) {
    this.modules = modules;
  }
  require(name) {
    return Debug.loadModule(name, this.modules);
  }
}

var loader = new ModuleLoader({
  'module42.js': `
    export default 42;
  `,
  'module1.js': `
    import "module2.js";

    export var foo = 0;
    var bar = 0;
    export var baz, qux;

    export function named() {
      foo++;
    }

    function declared1() {
      bar++;
    }

    function declared2() {
      bar += 2;
    }

    export {declared1};
    export {declared2};
    export {declared2 as declared3};

    export const cbar = 42;
    export let lbar = 4711;
  `,
  'module2.js': `
    export default function Default() {

    }

    export class ExportedClass {
    }
  `,
  'module3.js': `
    export default class ExportedClass {
    }
  `,
  'module4.js': `
    export default class {
    }
  `,
  'module5.js': `
    export default function() {
    }
  `,
  'module6.js': `
    import { cbar, lbar as bar } from "module1.js";
    import * as M2 from "module2.js";
    import fortyTwo from "module42.js";
    export * from "module3.js";
  `,
  'module7.js': `
    export { cbar, lbar as bar } from "module1.js";
  `,
});

var m42 = loader.require("module42.js");
var m1 = loader.require("module1.js");
var m2 = loader.require("module2.js");
var m3 = loader.require("module3.js");
var m4 = loader.require("module4.js");
var m5 = loader.require("module5.js");
var m6 = loader.require("module6.js");
var m6 = loader.require("module7.js");
