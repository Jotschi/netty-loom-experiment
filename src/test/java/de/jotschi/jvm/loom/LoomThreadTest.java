package de.jotschi.jvm.loom;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Test;

public class LoomThreadTest {

	static final int THREAD_COUNT = 1500;

	@Test
	public void testSelectorThreads() throws InterruptedException, TimeoutException {
		CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
		AtomicInteger startCounter = new AtomicInteger();
		for (int i = 0; i < THREAD_COUNT; i++) {
			Thread.startVirtualThread(() -> {
				try {
					System.out.println("Running: " + startCounter.incrementAndGet());
					latch.countDown();
					Selector selector = Selector.open();
					selector.select();
					Thread.sleep(10_000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

		if (!latch.await(5_000, TimeUnit.MILLISECONDS)) {
			dumpThreads();
		}
		assertEquals("Not all thread did start.", THREAD_COUNT, startCounter.get());
	}

	@Test
	public void testSelectorThreadsViaPlatform() throws InterruptedException, TimeoutException {
		CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
		AtomicInteger startCounter = new AtomicInteger();
		for (int i = 0; i < THREAD_COUNT; i++) {
			new Thread(() -> {
				try {
					System.out.println("Running: " + startCounter.incrementAndGet());
					latch.countDown();
					Selector selector = Selector.open();
					selector.select();
					Thread.sleep(10_000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		}

		if (!latch.await(5_000, TimeUnit.MILLISECONDS)) {
			dumpThreads();
		}
		assertEquals("Not all thread did start.", THREAD_COUNT, startCounter.get());
	}

	@Test
	public void testSimpleThreads() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
		AtomicInteger startCounter = new AtomicInteger();
		for (int i = 0; i < THREAD_COUNT; i++) {
			Thread.startVirtualThread(() -> {
				try {
					System.out.println("Running: " + startCounter.incrementAndGet());
					latch.countDown();
					Thread.sleep(1_000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
		if (!latch.await(5_000, TimeUnit.MILLISECONDS)) {
			dumpThreads();
		}
		assertEquals("Not all thread did run.", THREAD_COUNT, startCounter.get());
	}

	private void dumpThreads() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		ThreadInfo[] infos = bean.dumpAllThreads(true, true);
		System.out.println(Arrays.stream(infos).map(Object::toString).collect(Collectors.joining()));
	}

}
