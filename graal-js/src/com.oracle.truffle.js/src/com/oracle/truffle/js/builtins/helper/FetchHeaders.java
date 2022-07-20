package com.oracle.truffle.js.builtins.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FetchHeaders {

    private final Map<String, List<String>> headers;

    public FetchHeaders (Map<String, List<String>> init) {
        headers = init;
    }

    public FetchHeaders() {
        headers = new HashMap<>();
    }

    public void append(String name, String value) {
        headers.computeIfAbsent(name, v -> new ArrayList<>()).add(value);
    }

    public void delete(String name) {
        headers.remove(name);
    }

    public String get(String name) {
        return String.join(",", headers.get(name));
    }

    public boolean has(String name) {
        return headers.containsKey(name);
    }

    public void set(String name, String value) {
        headers.computeIfAbsent(name, v -> new ArrayList<>()).clear();
        headers.get(name).add(value);
    }
}
