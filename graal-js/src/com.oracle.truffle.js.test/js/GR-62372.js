/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the handling of a non-canonicalized zone-rules-based time zone.
 * 
 * @option js.zone-rules-based-time-zones
 */

load("assert.js");

assertSame('EST', new Intl.DateTimeFormat([], { timeZone: 'EST' }).resolvedOptions().timeZone);
