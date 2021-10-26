/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultVirtualThreadFactory;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.concurrent.VirtualThreadEventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Abstract base class for {@link EventLoopGroup} implementations that handles
 * their tasks by dispatching them via multiple virtual threads.
 */
public abstract class VirtualThreadEventLoopGroup extends VirtualThreadEventExecutorGroup implements EventLoopGroup {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultithreadEventLoopGroup.class);

	private static final int DEFAULT_EVENT_LOOP_THREADS;

	static {
		DEFAULT_EVENT_LOOP_THREADS = Math.max(1,
				SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));

		if (logger.isDebugEnabled()) {
			logger.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
		}
	}

	/**
	 * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int,
	 *      Executor, Object...)
	 */
	protected VirtualThreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
		super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
	}

	/**
	 * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int,
	 *      ThreadFactory, Object...)
	 */
	protected VirtualThreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
		super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
	}

	/**
	 * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int,
	 *      Executor, EventExecutorChooserFactory, Object...)
	 */
	protected VirtualThreadEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
			Object... args) {
		super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, chooserFactory, args);
	}

	@Override
	protected ThreadFactory newDefaultThreadFactory() {
		return new DefaultVirtualThreadFactory(getClass(), Thread.MAX_PRIORITY);
	}

	@Override
	public EventLoop next() {
		return (EventLoop) super.next();
	}

	@Override
	protected abstract EventLoop newChild(Executor executor, Object... args) throws Exception;

	@Override
	public ChannelFuture register(Channel channel) {
		return next().register(channel);
	}

	@Override
	public ChannelFuture register(ChannelPromise promise) {
		return next().register(promise);
	}

	@Deprecated
	@Override
	public ChannelFuture register(Channel channel, ChannelPromise promise) {
		return next().register(channel, promise);
	}

}
