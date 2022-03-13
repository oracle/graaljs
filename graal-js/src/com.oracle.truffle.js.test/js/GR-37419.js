/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option temporal
 */

load('assert.js');

let t = Temporal.Duration.from("\u2212P1Y");
assertSame(-1, t.years);
assertSame('-P1Y', t.toString());
