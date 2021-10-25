package io.netty.util.concurrent.loom;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.util.internal.ObjectUtil;

public class VirtualThreadPerTaskExecutor implements Executor {
	private final ThreadFactory threadFactory;
	private AtomicLong forkCount = new AtomicLong();

	public VirtualThreadPerTaskExecutor(ThreadFactory threadFactory) {
		this.threadFactory = ObjectUtil.checkNotNull(threadFactory, "threadFactory");
	}

	
	@Override
	public void execute(Runnable command) {
		System.out.format("Creating new thread %d\n", forkCount.incrementAndGet());
		threadFactory.newThread(command).start();
	}
}