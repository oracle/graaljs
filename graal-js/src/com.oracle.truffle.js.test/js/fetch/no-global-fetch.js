/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that none of the fetch globals are available by default.
 */

load('../assert.js');

assertFalse('fetch' in globalThis);
assertFalse('Headers' in globalThis);
assertFalse('Request' in globalThis);
assertFalse('Response' in globalThis);
