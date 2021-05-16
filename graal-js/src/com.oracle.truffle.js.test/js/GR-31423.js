/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test inspired by the pattern used in tests of various npm packages.
 * 
 * @option v8-compat
 */

// Used to throw ClassCastException
Function.bind(Function)();
new (Function.bind(Function));
