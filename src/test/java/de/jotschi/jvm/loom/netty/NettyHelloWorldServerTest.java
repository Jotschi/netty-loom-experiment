package de.jotschi.jvm.loom.netty;

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
		EventLoopGroup workerGroup =  new NioEventLoopGroup(THREAD_COUNT, virtualThreadFactory);
		startNetty(bossGroup, workerGroup);
	}
	
	@Test
	public void testPlatformThreads() throws InterruptedException {
		EventLoopGroup bossGroup = new NioEventLoopGroup(THREAD_COUNT);
		EventLoopGroup workerGroup = new NioEventLoopGroup(THREAD_COUNT);
		startNetty(bossGroup, workerGroup);
	}

	public void startNetty(EventLoopGroup bossGroup,  EventLoopGroup workerGroup) throws InterruptedException {
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