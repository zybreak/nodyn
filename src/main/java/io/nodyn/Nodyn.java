/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nodyn;

import io.netty.channel.EventLoopGroup;
import io.nodyn.crypto.CryptoInitializer;
import io.nodyn.loop.EventLoop;
import io.nodyn.runtime.NodynConfig;
import io.nodyn.runtime.Program;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Lance Ball
 */
public abstract class Nodyn {

    static {
        CryptoInitializer.initialize();
    }

    public static final String VERSION;
    public static final String NODE_VERSION;

    static {
        String v;
        try {
            v = loadVersion( "nodyn" );
        } catch (IOException e) {
            v = "Unknown";
        }
        VERSION = v;

        try {
            v = loadVersion("node" );
        } catch (IOException e) {
            v = "Unknown";
        }

        NODE_VERSION = v;
    }

    private static String loadVersion(String component) throws IOException {
        InputStream in = Nodyn.class.getClassLoader().getResourceAsStream(component + "-version.txt");
        BufferedReader reader = new BufferedReader( new InputStreamReader(in ) );
        String line = reader.readLine();
        reader.close();
        return line.trim();
    }

    protected static final String NODE_JS = "node.js";
    protected static final String PROCESS = "nodyn/process.js";

    abstract public Object loadBinding(String name);

    abstract public void handleThrowable(Throwable t);

    protected abstract NodeProcess initialize();

    abstract protected Object runScript(String script) throws IOException;

    // The following methods are used in contextify.js
    abstract public Object getGlobalContext();

    abstract public Program compile(String source, String fileName, boolean displayErrors) throws Throwable;

    abstract public void makeContext(Object global);

    abstract public boolean isContext(Object global);


    protected Nodyn(NodynConfig config, Vertx vertx, boolean controlLifeCycle) {
        EventLoopGroup elg = ((VertxInternal) vertx).getEventLoopGroup();
        this.eventLoop = new EventLoop(elg, controlLifeCycle);
        this.vertx = vertx;
        this.config = config;
        this.completionHandler = new CompletionHandler();
    }

    public int run() throws Throwable {
        start(null);
        return await();
    }

    /**
     * Starts nodyn asynchronously. The optional callback, if provided,
     * will be called when initialization has completed
     * @param callback optional - if provided will be called when initialization of nodyn completes
     */
    public void runAsync(Callback callback) {
        start(callback);
    }

    public NodynConfig getConfiguration() {
        return this.config;
    }

    public EventLoop getEventLoop() {
        return this.eventLoop;
    }

    public Vertx getVertx() {
        return this.vertx;
    }

    public void setExitHandler(ExitHandler handle) {
        this.exitHandler = handle;
    }

    public void reallyExit(int exitCode) {
        this.eventLoop.shutdown();
        if (this.exitHandler != null) {
            this.exitHandler.reallyExit(exitCode);
        } else {
            System.exit(exitCode);
        }
    }

    private int await() throws Throwable {
        this.eventLoop.await();

        if (this.completionHandler.error != null) {
            throw completionHandler.error;
        }

        if (this.completionHandler.process == null) {
            return -255;
        }

        return this.completionHandler.process.getExitCode();
    }

    private void start(final Callback callback) {
        this.eventLoop.submitUserTask(() -> {
			try {
				Nodyn.this.completionHandler.process = initialize();
				if (callback != null) {
					callback.call(CallbackResult.createSuccess());
				}
			} catch (Throwable t) {
				Nodyn.this.completionHandler.error = t;
				if (callback != null) {
					callback.call(CallbackResult.createError(t));
				}
			}
		}, "init");
    }

    private final EventLoop eventLoop;
    private final CompletionHandler completionHandler;
    private final Vertx vertx;
    private final NodynConfig config;
    private ExitHandler exitHandler;


    private static class CompletionHandler {
        public NodeProcess process;
        public Throwable error;
    }


}
