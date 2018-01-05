/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

/**
 * An ECMA Agent (see ECMA 8.7).
 */
public interface EcmaAgent {

    /**
     * Execute the given task on the Agent as requested by the owner agent.
     */
    void execute(EcmaAgent owner, Runnable task);

    /**
     * Returns true if the agent was terminated.
     */
    boolean isTerminated();

    /**
     * Terminate the agent.
     */
    void terminate(int timeout);

    /**
     * A factory to create Agent Clusters (see ECMA 8.8).
     *
     * Depending on the runtime configuration, agents in Graal.js can be Java threads (when running
     * in JS-only mode) as well as the uv event loops (when running in Node.js mode).
     *
     * Interop with Java is modeled around Agents, assuming that a Java thread is an isolated ECMA
     * Agent.
     *
     */
    public interface Factory {

        /**
         * Create a new Agent. After creation, the agent is typically executed in a new thread.
         */
        EcmaAgent createAgent(EcmaAgent parent);
    }

}
