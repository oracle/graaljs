/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

assertThrows(() => new Function("await import('test')"), SyntaxError, "await is only valid in async functions");
assertThrows(() => new Function("let test = await import('test')"), SyntaxError, "await is only valid in async functions");
