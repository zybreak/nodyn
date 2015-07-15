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

package io.nodyn.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.nodyn.NodeProcess;
import io.nodyn.netty.EOFEventHandler;
import io.nodyn.netty.UnrefHandler;
import io.nodyn.stream.StreamWrap;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author Bob McWhirter
 */
public class TCPWrap extends StreamWrap {

    private String addr;
    private int port = -1;

    public TCPWrap(NodeProcess process) {
        super(process, false);
    }

    public TCPWrap(NodeProcess process, int fd) throws Exception {
        super(process, false);
        SocketChannel socketChannel = UnsafeTcp.attach(fd);
        NioSocketChannel channel = new NioSocketChannel(socketChannel);
        this.channelFuture = channel.newSucceededFuture();
        channel.pipeline().addLast("emit.afterConnect", new AfterConnectEventHandler(this.process, TCPWrap.this));
        channel.pipeline().addLast("emit.eof", new EOFEventHandler(this.process, TCPWrap.this));
        channel.pipeline().addLast("handle", new UnrefHandler(this));
        process.getEventLoop().getEventLoopGroup().register(channel);
    }

    public TCPWrap(NodeProcess process, ChannelFuture channelFuture) {
        super(process, channelFuture);
    }

    public void bind(String addr, int port) {
        this.addr = addr;
        this.port = port;
    }

    public void listen(int backlog) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.process.getEventLoop().getEventLoopGroup());
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.config().setAutoRead(false);
//                ch.pipeline().addLast("debug", new DebugHandler("server"));
                ch.pipeline().addLast("emit.connection", new ConnectionEventHandler(TCPWrap.this.process, TCPWrap.this));
                ch.pipeline().addLast("handle", new UnrefHandler(TCPWrap.this));
            }
        });
        this.channelFuture = bootstrap.bind(this.addr, this.port);
        this.channelFuture.addListener(future -> {
			// TODO callback error
		});

        ref();
    }

    public void connect(String addr, int port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.process.getEventLoop().getEventLoopGroup());
        bootstrap.channel(NioSocketChannel.class);
        if (this.port >= 0) {
            if (this.addr != null) {
                bootstrap.localAddress(this.addr, this.port);
            } else {
                bootstrap.localAddress(this.port);
            }
        }

        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.config().setAutoRead(false);
                //ch.pipeline().addLast("debug", new DebugHandler("client"));
                ch.pipeline().addLast("emit.afterConnect", new AfterConnectEventHandler(TCPWrap.this.process, TCPWrap.this));
                ch.pipeline().addLast("emit.eof", new EOFEventHandler(TCPWrap.this.process, TCPWrap.this));
                ch.pipeline().addLast("handle", new UnrefHandler(TCPWrap.this));
            }
        });

        this.channelFuture = bootstrap.connect(addr, port);
        this.channelFuture.addListener(future -> {
			// TODO callback error
		});
        ref();
    }

    @Override
    public void shutdown() throws InterruptedException {
        ((NioSocketChannel) this.channelFuture.await().channel()).shutdownOutput();
    }

    public SocketAddress getRemoteAddress() throws InterruptedException {
        return this.channelFuture.await().channel().remoteAddress();
    }

    public SocketAddress getLocalAddress() throws InterruptedException {
        return this.channelFuture.await().channel().localAddress();
    }

    public int getFd() throws NoSuchFieldException, IllegalAccessException, IOException, InterruptedException {
        return UnsafeTcp.getFd((NioSocketChannel) this.channelFuture.await().channel());

    }
}
