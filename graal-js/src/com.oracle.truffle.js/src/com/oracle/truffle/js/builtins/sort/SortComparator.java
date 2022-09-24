package com.oracle.truffle.js.builtins.sort;

import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantByteArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantIntArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.Comparator;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;

public class SortComparator implements Comparator<Object> {
    private final Object compFnObj;
    private final boolean isFunction;

    public SortComparator(Object compFnObj) {
        this.compFnObj = compFnObj;
        this.isFunction = JSFunction.isJSFunction(compFnObj);
    }

    @Override
    public int compare(Object arg0, Object arg1) {
        if (arg0 == Undefined.instance) {
            if (arg1 == Undefined.instance) {
                return 0;
            }
            return 1;
        } else if (arg1 == Undefined.instance) {
            return -1;
        }
        Object retObj;
        if (isFunction) {
            retObj = JSFunction.call((JSFunctionObject) compFnObj, Undefined.instance, new Object[]{arg0, arg1});
        } else {
            retObj = JSRuntime.call(compFnObj, Undefined.instance, new Object[]{arg0, arg1});
        }
        return convertResult(retObj);
    }

    private int convertResult(Object retObj) {
        if (retObj instanceof Integer) {
            return (int) retObj;
        } else {
            double d = JSRuntime.toDouble(retObj);
            if (d < 0) {
                return -1;
            } else if (d > 0) {
                return 1;
            } else {
                // +/-0 or NaN
                return 0;
            }
        }
    }

    public static Comparator<Object> getDefaultComparator(Object thisObj, boolean isTypedArrayImplementation) {
        if (isTypedArrayImplementation) {
            return null; // use Comparable.compareTo (equivalent to Comparator.naturalOrder())
        } else {
            if (JSArray.isJSArray(thisObj)) {
                ScriptArray array = arrayGetArrayType((JSDynamicObject) thisObj);
                if (array instanceof AbstractIntArray || array instanceof ConstantByteArray || array instanceof ConstantIntArray) {
                    return JSArray.DEFAULT_JSARRAY_INTEGER_COMPARATOR;
                } else if (array instanceof AbstractDoubleArray || array instanceof ConstantDoubleArray) {
                    return JSArray.DEFAULT_JSARRAY_DOUBLE_COMPARATOR;
                }
            }
            return JSArray.DEFAULT_JSARRAY_COMPARATOR;
        }
    }
}
