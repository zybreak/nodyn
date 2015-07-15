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

import io.nodyn.extension.ExtensionLoader;
import io.nodyn.fs.UnsafeFs;
import io.nodyn.loop.EventLoop;
import io.nodyn.loop.ImmediateCheckHandle;
import io.nodyn.posix.NodePosixHandler;
import io.vertx.core.Vertx;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class NodeProcess extends EventSource {

    private final Map<String, Object> bindings = new HashMap<>();

    private final Nodyn nodyn;
    private final String osName;
    private final String osArch;
    private final POSIX posix;

    private ImmediateCheckHandle immediateCheckHandle;
    private boolean needImmediateCallback;
    private int exitCode = 0;

    private Runnable tickCallback;

    private ExtensionLoader extensionLoader;

    public NodeProcess(Nodyn nodyn) {
        this(nodyn, System.getProperties());
    }

    public NodeProcess(Nodyn nodyn, Properties props) {
        this.nodyn = nodyn;
        this.osName = props.getProperty("os.name").toLowerCase();
        this.osArch = props.getProperty("os.arch").toLowerCase();

        this.immediateCheckHandle = new ImmediateCheckHandle(nodyn.getEventLoop(),
                    () -> emit("checkImmediate", CallbackResult.EMPTY_SUCCESS));

        this.posix = POSIXFactory.getPOSIX(new NodePosixHandler(), true);

        // TODO remove this DynJS-specific code.
        this.extensionLoader = new ExtensionLoader( nodyn.getConfiguration().getClassLoader() );
    }

    public Object jaropen(String filename) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, IOException {
        return this.extensionLoader.load( filename );
    }

    public long getPid() {
        return getPosix().getpid();
    }

    public boolean isatty(int fd) throws NoSuchFieldException, IllegalAccessException {
        return this.posix.isatty(UnsafeFs.createFileDescriptor( fd ) );
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public void reallyExit() {
        this.nodyn.reallyExit(this.exitCode);
    }

    public int getExitCode() {
        return this.exitCode;
    }

    public void setupNextTick(Runnable tickCallback) {
        this.tickCallback = tickCallback;
        doNextTick();
    }

    public void doNextTick() {
        this.tickCallback.run();
    }

    public boolean getNeedImmediateCallback() {
        return this.needImmediateCallback;
    }

    public void setNeedImmediateCallback(boolean v) {
        if (this.immediateCheckHandle.isActive() == v) {
            return;
        }
        this.needImmediateCallback = v;
        if (v) {
            this.immediateCheckHandle.start();
        } else {
            this.immediateCheckHandle.stop();
        }
    }

    public Nodyn getNodyn() {
        return this.nodyn;
    }

    public EventLoop getEventLoop() {
        return this.nodyn.getEventLoop();
    }

    public Vertx getVertx() {
        return this.nodyn.getVertx();
    }

    public Object binding(String name) {
        Object binding = this.bindings.get(name);
        if (binding == null) {
            binding = loadBinding(name);
            this.bindings.put(name, binding);
        }
        return binding;
    }

    protected Object loadBinding(String name) {
        return this.nodyn.loadBinding(name);
    }

    public String getArgv0() {
        String bin = System.getProperty("nodyn.binary");
        if (bin == null) {
            bin = "nodyn";
        }
        return bin;
    }

    public String getExecPath() {
        String bin = System.getProperty("nodyn.binary");
        if (bin == null) {
            bin = "nodyn";
        }
        File nodynBinary = new File(bin);
        nodynBinary = nodynBinary.getAbsoluteFile();
        return nodynBinary.getAbsolutePath();
    }

    /**
     * http://nodejs.org/api/process.html#process_process_platform 'darwin',
     * 'freebsd', 'linux', 'sunos' or 'win32'
     *
     * @return
     */
    public String platform() {
        if (isLinux()) {
            return "linux";
        } else if (isMac()) {
            return "darwin";
        } else if (isFreeBSD()) {
            return "freebsd";
        } else if (isSunos()) {
            return "sunos";
        } else if (isWindows()) {
            return "win32";
        }
        return null;
    }

    public boolean isLinux() {
        return osName.contains("linux");
    }

    public boolean isMac() {
        return osName.contains("darwin") || osName.contains("mac");
    }

    public boolean isFreeBSD() {
        return osName.contains("freebsd");
    }

    public boolean isSunos() {
        return osName.contains("sunos");
    }

    public boolean isWindows() {
        return osName.contains("win");
    }

    /**
     * http://nodejs.org/api/process.html#process_process_arch 'arm', 'ia32', or
     * 'x64'
     *
     * @return
     */
    public String arch() {
        if (isX64()) {
            return "x64";
        } else if (isIa32()) {
            return "ia32";
        } else if (isArm()) {
            return "arm";
        }
        return null;
    }

    public boolean isIa32() {
        return osArch.contains("x86") || osArch.contains("i386");
    }

    public boolean isX64() {
        return (osArch.contains("amd64")) || (osArch.contains("x86_64"));
    }

    public boolean isArm() {
        return osArch.contains("arm");
    }

    public POSIX getPosix() {
        return this.posix;
    }

}
