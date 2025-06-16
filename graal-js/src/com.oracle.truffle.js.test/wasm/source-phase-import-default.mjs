/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that source phase imports are enabled by default if js.webassembly=true.
 *
 * @option webassembly=true
 * @option unhandled-rejections=throw
 */

import "./source-phase-import.mjs";
import "./source-phase-import-dynamic.mjs";
