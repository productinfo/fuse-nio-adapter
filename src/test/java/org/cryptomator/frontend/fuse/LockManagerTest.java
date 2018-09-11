package org.cryptomator.frontend.fuse;

import org.cryptomator.frontend.fuse.LockManager.PathLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LockManagerTest {

	static {
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss.SSS");
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
	}

	private static final Logger LOG = LoggerFactory.getLogger(LockManagerTest.class);

	@Test
	public void testLockCountDuringLock() {
		LockManager lockManager = new LockManager();
		Assertions.assertFalse(lockManager.isLocked("/foo"));
		Assertions.assertFalse(lockManager.isLocked("/foo/bar"));
		Assertions.assertFalse(lockManager.isLocked("/foo/bar/baz"));
		try (PathLock lock1 = lockManager.lockPathForReading("/foo/bar/baz")) {
			Assertions.assertTrue(lockManager.isLocked("/foo"));
			Assertions.assertTrue(lockManager.isLocked("/foo/bar"));
			Assertions.assertTrue(lockManager.isLocked("/foo/bar/baz"));
			try (PathLock lock2 = lockManager.lockPathForReading("/foo/bar/baz")) {
				Assertions.assertTrue(lockManager.isLocked("/foo"));
				Assertions.assertTrue(lockManager.isLocked("/foo/bar"));
				Assertions.assertTrue(lockManager.isLocked("/foo/bar/baz"));
			}
			Assertions.assertTrue(lockManager.isLocked("/foo"));
			Assertions.assertTrue(lockManager.isLocked("/foo/bar"));
			Assertions.assertTrue(lockManager.isLocked("/foo/bar/baz"));
			try (PathLock lock3 = lockManager.lockPathForReading("/foo/bar/baz")) {
				Assertions.assertTrue(lockManager.isLocked("/foo"));
				Assertions.assertTrue(lockManager.isLocked("/foo/bar"));
				Assertions.assertTrue(lockManager.isLocked("/foo/bar/baz"));
			}
			Assertions.assertTrue(lockManager.isLocked("/foo"));
			Assertions.assertTrue(lockManager.isLocked("/foo/bar"));
			Assertions.assertTrue(lockManager.isLocked("/foo/bar/baz"));
		}
		Assertions.assertFalse(lockManager.isLocked("/foo"));
		Assertions.assertFalse(lockManager.isLocked("/foo/bar"));
		Assertions.assertFalse(lockManager.isLocked("/foo/bar/baz"));
	}

	@Test
	public void testMultipleReadLocks() throws InterruptedException {
		LockManager lockManager = new LockManager();
		int numThreads = 8;
		ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		CountDownLatch done = new CountDownLatch(numThreads);
		AtomicInteger counter = new AtomicInteger();
		AtomicInteger maxCounter = new AtomicInteger();

		for (int i = 0; i < numThreads; i++) {
			int threadnum = i;
			threadPool.submit(() -> {
				try (PathLock lock = lockManager.lockPathForReading("/foo/bar/baz")) {
					LOG.debug("ENTER thread {}", threadnum);
					counter.incrementAndGet();
					Thread.sleep(50);
					maxCounter.set(Math.max(counter.get(), maxCounter.get()));
					counter.decrementAndGet();
					LOG.debug("LEAVE thread {}", threadnum);
				} catch (InterruptedException e) {
					LOG.error("thread interrupted", e);
				}
				done.countDown();
			});
		}

		done.await();
		Assertions.assertEquals(numThreads, maxCounter.get());
	}

	@Test
	public void testMultipleWriteLocks() throws InterruptedException {
		LockManager lockManager = new LockManager();
		int numThreads = 8;
		ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		CountDownLatch done = new CountDownLatch(numThreads);
		AtomicInteger counter = new AtomicInteger();
		AtomicInteger maxCounter = new AtomicInteger();

		for (int i = 0; i < numThreads; i++) {
			int threadnum = i;
			threadPool.submit(() -> {
				try (PathLock lock = lockManager.lockPathForWriting("/foo/bar/baz")) {
					LOG.debug("ENTER thread {}", threadnum);
					counter.incrementAndGet();
					Thread.sleep(10);
					maxCounter.set(Math.max(counter.get(), maxCounter.get()));
					counter.decrementAndGet();
					LOG.debug("LEAVE thread {}", threadnum);
				} catch (InterruptedException e) {
					LOG.error("thread interrupted", e);
				}
				done.countDown();
			});
		}

		done.await();
		Assertions.assertEquals(1, maxCounter.get());
	}

}