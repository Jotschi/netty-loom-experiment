package io.netty.util.concurrent.loom;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class VirtualThreadFactory implements ThreadFactory {

	private AtomicLong forkCount = new AtomicLong();
	public ThreadFactory factory = Thread.ofVirtual().name("worker", 0).factory();

	public Thread newThread(Runnable r) {
		System.out.format("Creating new thread %d\n", forkCount.incrementAndGet());
		return factory.newThread(r);
	}
}
