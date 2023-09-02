/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test of the handling of yield and await in a dead code.
 */

// Just ensure that we do not run into an internal error.

(function* () { while (false) yield })().next();

(async function () { while (false) await 42; })();
