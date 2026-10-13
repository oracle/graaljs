/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option crypto=true
 * @option webassembly=true
 */

var memory = new WebAssembly.Memory({initial: 1});
var wasmView = new Uint8Array(memory.buffer, 1, 4);
crypto.getRandomValues(wasmView);
