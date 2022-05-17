/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 * @option js.property-cache-limit=0
 */

load('../assert.js')


//---------------------------------------------//
class C { static accessor x; }

assertThrows(() => {
    Object.getOwnPropertyDescriptor(C, 'x').get.call(Object.create(C)) // should throw
});
assertThrows(() => {
    Object.getOwnPropertyDescriptor(C, 'x').set.call(Object.create(C)) // should throw
});
