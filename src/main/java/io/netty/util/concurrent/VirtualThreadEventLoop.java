package io.netty.util.concurrent;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.UnstableApi;

public abstract class VirtualThreadEventLoop extends VirtualThreadEventExecutor implements EventLoop {
	protected static final int DEFAULT_MAX_PENDING_TASKS = Math.max(16,
			SystemPropertyUtil.getInt("io.netty.eventLoop.maxPendingTasks", Integer.MAX_VALUE));

	private final Queue<Runnable> tailTasks;

	protected VirtualThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory, boolean addTaskWakesUp) {
	        this(parent, threadFactory, addTaskWakesUp, DEFAULT_MAX_PENDING_TASKS, LoomRejectedExecutionHandlers.reject());
	    }

	protected VirtualThreadEventLoop(EventLoopGroup parent, Executor executor, boolean addTaskWakesUp) {
	        this(parent, executor, addTaskWakesUp, DEFAULT_MAX_PENDING_TASKS, LoomRejectedExecutionHandlers.reject());
	    }

	protected VirtualThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory,
	                                    boolean addTaskWakesUp, int maxPendingTasks,
	                                    LoomRejectedExecutionHandler rejectedExecutionHandler) {
	        super(parent, threadFactory, addTaskWakesUp, maxPendingTasks, rejectedExecutionHandler);
	        tailTasks = newTaskQueue(maxPendingTasks);
	    }

	protected VirtualThreadEventLoop(EventLoopGroup parent, Executor executor,
	                                    boolean addTaskWakesUp, int maxPendingTasks,
	                                    LoomRejectedExecutionHandler rejectedExecutionHandler) {
	        super(parent, executor, addTaskWakesUp, maxPendingTasks, rejectedExecutionHandler);
	        tailTasks = newTaskQueue(maxPendingTasks);
	    }

	protected VirtualThreadEventLoop(EventLoopGroup parent, Executor executor,
	                                    boolean addTaskWakesUp, Queue<Runnable> taskQueue, Queue<Runnable> tailTaskQueue,
	                                    LoomRejectedExecutionHandler rejectedExecutionHandler) {
	        super(parent, executor, addTaskWakesUp, taskQueue, rejectedExecutionHandler);
	        tailTasks = ObjectUtil.checkNotNull(tailTaskQueue, "tailTaskQueue");
	    }

	@Override
	public EventLoopGroup parent() {
		return (EventLoopGroup) super.parent();
	}

	@Override
	public EventLoop next() {
		return (EventLoop) super.next();
	}

	@Override
	public ChannelFuture register(Channel channel) {
		return register(new DefaultChannelPromise(channel, this));
	}

	@Override
	public ChannelFuture register(final ChannelPromise promise) {
		ObjectUtil.checkNotNull(promise, "promise");
		promise.channel().unsafe().register(this, promise);
		return promise;
	}

	@Deprecated
	@Override
	public ChannelFuture register(final Channel channel, final ChannelPromise promise) {
		ObjectUtil.checkNotNull(promise, "promise");
		ObjectUtil.checkNotNull(channel, "channel");
		channel.unsafe().register(this, promise);
		return promise;
	}

	/**
	 * Adds a task to be run once at the end of next (or current) {@code eventloop}
	 * iteration.
	 *
	 * @param task to be added.
	 */
	@UnstableApi
	public final void executeAfterEventLoopIteration(Runnable task) {
		ObjectUtil.checkNotNull(task, "task");
		if (isShutdown()) {
			reject();
		}

		if (!tailTasks.offer(task)) {
			reject(task);
		}

		if (!(task instanceof LazyRunnable) && wakesUpForTask(task)) {
			wakeup(inEventLoop());
		}
	}

	/**
	 * Removes a task that was added previously via
	 * {@link #executeAfterEventLoopIteration(Runnable)}.
	 *
	 * @param task to be removed.
	 *
	 * @return {@code true} if the task was removed as a result of this call.
	 */
	@UnstableApi
	final boolean removeAfterEventLoopIterationTask(Runnable task) {
		return tailTasks.remove(ObjectUtil.checkNotNull(task, "task"));
	}

	@Override
	protected void afterRunningAllTasks() {
		runAllTasksFrom(tailTasks);
	}

	@Override
	protected boolean hasTasks() {
		return super.hasTasks() || !tailTasks.isEmpty();
	}

	@Override
	public int pendingTasks() {
		return super.pendingTasks() + tailTasks.size();
	}

	/**
	 * Returns the number of {@link Channel}s registered with this {@link EventLoop}
	 * or {@code -1} if operation is not supported. The returned value is not
	 * guaranteed to be exact accurate and should be viewed as a best effort.
	 */
	@UnstableApi
	public int registeredChannels() {
		return -1;
	}

}
