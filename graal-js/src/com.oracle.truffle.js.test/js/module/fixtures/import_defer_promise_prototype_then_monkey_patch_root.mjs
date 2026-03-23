/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import "./import_defer_promise_prototype_then_monkey_patch_dep.mjs";

globalThis.importDeferThenLog.push("root");

export const value = 42;
