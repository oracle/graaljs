/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

'use strict';

exports.originalThis = this;
this.value = 42;
module.exports = { originalExports: exports };
module.exports.receiverAfterReassignment = this;
