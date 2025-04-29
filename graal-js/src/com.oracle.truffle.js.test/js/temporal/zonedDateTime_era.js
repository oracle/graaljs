/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option temporal=true
 */

load('../assert.js');

// iso8601 calendar
assertSame(undefined, Temporal.Now.zonedDateTimeISO().era);
assertSame(undefined, Temporal.Now.zonedDateTimeISO().eraYear);
