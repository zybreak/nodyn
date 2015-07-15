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

package io.nodyn.netty.pipe;

import io.netty.buffer.ByteBuf;
import io.netty.channel.FileRegion;
import io.nodyn.NodeProcess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

/**
 * @author Bob McWhirter
 */
public class NioDuplexStreamChannel extends AbstractNioStreamChannel {

    private final InputStream in;
    private final OutputStream out;
    private final Pipe inPipe;
    private final Pipe outPipe;

    public static NioDuplexStreamChannel create(NodeProcess process, InputStream in, OutputStream out) throws IOException {
        Pipe inPipe = Pipe.open();
        Pipe outPipe = Pipe.open();
        return new NioDuplexStreamChannel(process, in, inPipe, out, outPipe);
    }

    protected NioDuplexStreamChannel(NodeProcess process, InputStream in, Pipe inPipe, OutputStream out, Pipe outPipe) {
        super(process, inPipe);
        this.in = in;
        this.inPipe = inPipe;
        this.out = out;
        this.outPipe = outPipe;
        startPumps();
    }

    @Override
    protected Pipe.SourceChannel javaChannel() {
        return (Pipe.SourceChannel) super.javaChannel();
    }

    protected void startPumps() {
        this.process.getEventLoop().submitBlockingTask(() -> {
			byte[] buf = new byte[1024];
			int numRead = 0;
			try {
				while ((numRead = NioDuplexStreamChannel.this.in.read(buf)) >= 0) {
					if ( numRead > 0 ) {
						NioDuplexStreamChannel.this.inPipe.sink().write(ByteBuffer.wrap(buf, 0, numRead));
					}
				}
				NioDuplexStreamChannel.this.inPipe.sink().close();
			} catch (IOException e) {
				NioDuplexStreamChannel.this.process.getNodyn().handleThrowable(e);
				try {
					NioDuplexStreamChannel.this.inPipe.sink().close();
				} catch (IOException e1) {
					NioDuplexStreamChannel.this.process.getNodyn().handleThrowable(e);
				}
			}
		});

        this.process.getEventLoop().submitBlockingTask(() -> {
			ByteBuffer buf = ByteBuffer.allocate(1024);
			int numRead = 0;
			try {
				while ((numRead = NioDuplexStreamChannel.this.outPipe.source().read(buf)) >= 0) {
					if (numRead > 0) {
						byte[] writeMe = buf.array();
						NioDuplexStreamChannel.this.out.write(writeMe, 0, numRead);
						NioDuplexStreamChannel.this.out.flush();
						buf.clear();
					}
				}
			} catch (IOException e) {
				NioDuplexStreamChannel.this.process.getNodyn().handleThrowable(e);
				try {
					NioDuplexStreamChannel.this.outPipe.source().close();
				} catch (IOException e1) {
					NioDuplexStreamChannel.this.process.getNodyn().handleThrowable(e);
				}
			}
		});
    }

    @Override
    protected long doWriteFileRegion(FileRegion region) throws Exception {
        return 0;
    }

    @Override
    protected int doReadBytes(ByteBuf byteBuf) throws Exception {
        return byteBuf.writeBytes(javaChannel(), byteBuf.writableBytes());
    }

    @Override
    protected int doWriteBytes(ByteBuf buf) throws Exception {
        final int expectedWrittenBytes = buf.readableBytes();
        return buf.readBytes(this.outPipe.sink(), expectedWrittenBytes);
    }

    @Override
    protected void doClose() throws Exception {
        this.inPipe.source().close();
        this.outPipe.sink().close();
    }

    @Override
    public boolean isActive() {
        //return this.inPipe.source().isOpen();
        return true;
    }

}
