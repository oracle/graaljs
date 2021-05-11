/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test inspired by the pattern used in swagger-client (npm package).
 * 
 * @option v8-compat
 */

// Used to throw an internal error
Promise.resolve('return 6*7;').then(Function);
