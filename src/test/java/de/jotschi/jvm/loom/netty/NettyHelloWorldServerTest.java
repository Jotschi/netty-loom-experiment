package de.jotschi.jvm.loom.netty;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.ThreadFactory;

import org.junit.Test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.LoomNioEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.loom.VirtualThreadFactory;

public class NettyHelloWorldServerTest {

	static final int PORT = 8080;
	static final int THREAD_COUNT = 100;

	@Test
	public void testVirtualThreads() throws InterruptedException {
		ThreadFactory virtualThreadFactory = new VirtualThreadFactory();
		EventLoopGroup bossGroup = new LoomNioEventLoopGroup(THREAD_COUNT, virtualThreadFactory);
		EventLoopGroup workerGroup = new LoomNioEventLoopGroup(THREAD_COUNT, virtualThreadFactory);
		startNetty(bossGroup, workerGroup);
	}

	@Test
	public void testCustomEventLoopGroup() throws InterruptedException {
		ThreadFactory virtualThreadFactory = new VirtualThreadFactory();
		EventLoopGroup bossGroup = new NioEventLoopGroup(THREAD_COUNT, virtualThreadFactory);
		EventLoopGroup workerGroup = new NioEventLoopGroup(THREAD_COUNT, virtualThreadFactory);
		startNetty(bossGroup, workerGroup);
	}

	@Test
	public void testCustomSelectorProvider() throws InterruptedException {
		ThreadFactory virtualThreadFactory = new VirtualThreadFactory();
		SelectorProvider provider = new SelectorProvider() {

			private SelectorProvider selector = SelectorProvider.provider();

			@Override
			public java.nio.channels.SocketChannel openSocketChannel() throws IOException {
				return selector.openSocketChannel();
			}

			@Override
			public ServerSocketChannel openServerSocketChannel() throws IOException {
				return selector.openServerSocketChannel();
			}

			@Override
			public AbstractSelector openSelector() throws IOException {
				return selector.openSelector();
			}

			@Override
			public Pipe openPipe() throws IOException {
				return selector.openPipe();
			}

			@Override
			public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
				return selector.openDatagramChannel(family);
			}

			@Override
			public DatagramChannel openDatagramChannel() throws IOException {
				return selector.openDatagramChannel();
			}
		};
		System.out.println(provider.getClass().getName());
		EventLoopGroup bossGroup = new LoomNioEventLoopGroup(THREAD_COUNT, virtualThreadFactory, provider);
		EventLoopGroup workerGroup = new LoomNioEventLoopGroup(THREAD_COUNT, virtualThreadFactory, provider);
		startNetty(bossGroup, workerGroup);
	}

	@Test
	public void testPlatformThreads() throws InterruptedException {
		EventLoopGroup bossGroup = new NioEventLoopGroup(THREAD_COUNT);
		EventLoopGroup workerGroup = new NioEventLoopGroup(THREAD_COUNT);
		startNetty(bossGroup, workerGroup);
	}

	public void startNetty(EventLoopGroup bossGroup, EventLoopGroup workerGroup) throws InterruptedException {
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup);
			b.option(ChannelOption.SO_BACKLOG, 1024);
			b.channel(NioServerSocketChannel.class);
			b.handler(new LoggingHandler(LogLevel.DEBUG));
			b.childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();
					p.addLast(new HttpServerCodec());
					p.addLast(new HttpServerExpectContinueHandler());
					p.addLast(new HttpHelloWorldServerHandler());
				}

			});

			Channel ch = b.bind(PORT).sync().channel();

			System.err.println("Open your web browser and navigate to http://127.0.0.1:" + PORT + '/');

			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}