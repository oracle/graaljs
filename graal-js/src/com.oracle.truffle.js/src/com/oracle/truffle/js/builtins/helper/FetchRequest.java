package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

import java.net.MalformedURLException;
import java.net.URL;

public class FetchRequest {
    private final int maxFollow = 20;

    private URL url;
    private TruffleString method;
    private int redirectCount;
    private FetchHeaders headers;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public TruffleString getMethod() {
        return method;
    }

    public int getRedirectCount() {
        return redirectCount;
    }

    public void incrementRedirectCount() throws Exception {
        if (redirectCount < maxFollow) {
            redirectCount++;
        } else {
            throw new Exception();
        }
    }

    public FetchRequest(TruffleString input, JSObject init) throws MalformedURLException {
        this.url = new URL(input.toString());

        if (url.getUserInfo() != null) {
            throw Errors.createFetchError(url + " includes embedded credentials\"");
        }

        TruffleString k = TruffleString.fromJavaStringUncached("method", TruffleString.Encoding.UTF_8);
        if (JSObject.hasProperty(init, k)) {
            method = (TruffleString) JSObject.get(init, k);
        } else {
            method = TruffleString.fromJavaStringUncached("GET", TruffleString.Encoding.UTF_8);
        }
    }
}
