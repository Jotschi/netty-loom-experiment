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
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.LoomNioEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.loom.VirtualThreadFactory;

public class NettyHelloWorldServerTest {

  static final int PORT = 8080;
  /**
   * Thread count for group which will accept connections
   */
  static final int PARENT_THREAD_COUNT = 10;

  /**
   * Thread count for group which will handle accepted connections
   */
  static final int WORKER_THREAD_COUNT = 100;

  @Test
  public void testVirtualThreads() throws InterruptedException {
    ThreadFactory virtualThreadFactory = new VirtualThreadFactory();
    EventLoopGroup bossGroup = new LoomNioEventLoopGroup(8, virtualThreadFactory);
    EventLoopGroup workerGroup = new LoomNioEventLoopGroup(32, virtualThreadFactory);
    startNetty(bossGroup, workerGroup, NioServerSocketChannel.class);
  }

  @Test
  public void testMixedVirtualThreads() throws InterruptedException {
    ThreadFactory virtualThreadFactory = new VirtualThreadFactory();
    EventLoopGroup bossGroup = new NioEventLoopGroup(PARENT_THREAD_COUNT);
    EventLoopGroup workerGroup = new NioEventLoopGroup(WORKER_THREAD_COUNT, virtualThreadFactory);
    startNetty(bossGroup, workerGroup, NioServerSocketChannel.class);
  }

  @Test
  public void testCustomEventLoopGroup() throws InterruptedException {
    ThreadFactory virtualThreadFactory = new VirtualThreadFactory();
    EventLoopGroup bossGroup = new NioEventLoopGroup(PARENT_THREAD_COUNT, virtualThreadFactory);
    EventLoopGroup workerGroup = new NioEventLoopGroup(WORKER_THREAD_COUNT, virtualThreadFactory);
    startNetty(bossGroup, workerGroup, NioServerSocketChannel.class);
  }

  @Test
  public void testIOUringLoopGroupWithVirtual() throws InterruptedException {
    // 440k: T:200, wrk -d 20 -t 8 -c 4000
    ThreadFactory virtualThreadFactory = new VirtualThreadFactory();
    EventLoopGroup bossGroup = new IOUringEventLoopGroup(2, virtualThreadFactory);
    EventLoopGroup workerGroup = new IOUringEventLoopGroup(WORKER_THREAD_COUNT, virtualThreadFactory);
    startNetty(bossGroup, workerGroup, IOUringServerSocketChannel.class);
  }

  @Test
  public void testIOUringWithMixedSupport() throws InterruptedException {
    ThreadFactory virtualThreadFactory = new VirtualThreadFactory();
    EventLoopGroup bossGroup = new IOUringEventLoopGroup(2);
    EventLoopGroup workerGroup = new IOUringEventLoopGroup(WORKER_THREAD_COUNT, virtualThreadFactory);
    startNetty(bossGroup, workerGroup, IOUringServerSocketChannel.class);
  }

  @Test
  public void testIOUringLoopGroupWithPlatform() throws InterruptedException {
    // 254k
    ThreadFactory platformThreadFactory = new DefaultThreadFactory("netty");
    EventLoopGroup bossGroup = new IOUringEventLoopGroup(PARENT_THREAD_COUNT, platformThreadFactory);
    EventLoopGroup workerGroup = new IOUringEventLoopGroup(WORKER_THREAD_COUNT, platformThreadFactory);
    startNetty(bossGroup, workerGroup, IOUringServerSocketChannel.class);
  }

  @Test
  public void testPlatformThreads() throws InterruptedException {
    // 268k
    EventLoopGroup bossGroup = new NioEventLoopGroup(PARENT_THREAD_COUNT);
    EventLoopGroup workerGroup = new NioEventLoopGroup(WORKER_THREAD_COUNT);
    startNetty(bossGroup, workerGroup, NioServerSocketChannel.class);
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
    EventLoopGroup bossGroup = new LoomNioEventLoopGroup(PARENT_THREAD_COUNT, virtualThreadFactory, provider);
    EventLoopGroup workerGroup = new LoomNioEventLoopGroup(WORKER_THREAD_COUNT, virtualThreadFactory, provider);
    startNetty(bossGroup, workerGroup, NioServerSocketChannel.class);
  }

  public void startNetty(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
    Class<? extends ServerChannel> channelClazz) throws InterruptedException {
    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup);
      b.option(ChannelOption.SO_BACKLOG, 1024);
      b.channel(channelClazz);
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