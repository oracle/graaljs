/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test for checking the order of properties in AggregateError.
 * 
 * @option ecmascript-version=2021
 */

load('assert.js');

let propertyNames = Object.getOwnPropertyNames(new AggregateError('msg', []));

assertSame(3, propertyNames.length);
assertSame('message', propertyNames[0]);
assertSame('errors', propertyNames[1]);
assertSame('stack', propertyNames[2]);

true;
