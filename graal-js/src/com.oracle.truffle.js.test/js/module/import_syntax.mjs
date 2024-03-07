/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import { one as eins } from "./fixtures/export_name.mjs";
import { 'onePlusOne' as zwei } from "./fixtures/export_name.mjs";
import { "onePlusTwo" as drei } from "./fixtures/export_name.mjs";

if (eins + zwei + drei !== 6) throw new Error();
