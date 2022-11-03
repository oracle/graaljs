/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verify the Iterator and AsyncIterator constructors throw if NewTarget is invalid.
 *
 * @option iterator-helpers=true
 */

load("../assert.js");

// Iterator constructors are abstract; NewTarget must not be undefined or the constructor itself.
assertThrows(() => { new Iterator(); }, TypeError);
assertThrows(() => { Reflect.construct(Iterator, []); }, TypeError);
assertThrows(() => { Reflect.construct(Iterator, [], undefined); }, TypeError);
assertThrows(() => { Reflect.construct(Iterator, [], Iterator); }, TypeError);

assertThrows(() => { new AsyncIterator(); }, TypeError);
assertThrows(() => { Reflect.construct(AsyncIterator, []); }, TypeError);
assertThrows(() => { Reflect.construct(AsyncIterator, [], undefined); }, TypeError);
assertThrows(() => { Reflect.construct(AsyncIterator, [], AsyncIterator); }, TypeError);

// Iterator constructors cannot be called.
assertThrows(() => { Iterator(); }, TypeError);
assertThrows(() => { Reflect.apply(Iterator, undefined, []); }, TypeError);

assertThrows(() => { AsyncIterator(); }, TypeError);
assertThrows(() => { Reflect.apply(AsyncIterator, undefined, []); }, TypeError);
