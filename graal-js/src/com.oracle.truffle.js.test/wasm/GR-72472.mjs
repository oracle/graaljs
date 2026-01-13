/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test of the import of re-exported source phase import.
 *
 * @option webassembly=true
 */

import { mod as indirect } from "./source-phase-import-reexport.mjs";
import source direct from "./source-phase-import.wasm";

load('../js/assert.js');

assertTrue(indirect instanceof WebAssembly.Module);
assertSame(direct, indirect);
