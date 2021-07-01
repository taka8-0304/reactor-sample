package taka8.sample.reactor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyMonoTest {

	private static final Logger __logger = LoggerFactory.getLogger(MyMonoTest.class);

	@Test
	public void testMapping() {
		var mono = MyMono.create(s -> {
			s.success("good");
		}).map(v -> {
			return v + "_mod";
		});
		mono.subscribe(v -> {
			Assertions.assertEquals("good_mod", v);
		});
	}

	@Test
	public void testException() {
		var mono = MyMono.create(s -> {
			throw new RuntimeException("Error.");
		}).map(v -> {
			return v + "_mod";
		});
		try {
			mono.subscribe(v -> {
			});
		} catch (RuntimeException e) {
			__logger.info("Dump stacktrace", e);
		}
	}

	@Test
	public void testAsync() throws Exception {
		var executor = Executors.newSingleThreadExecutor();
		var mono = MyMono.create(s -> {
			executor.execute(() -> {
				__logger.info("create CALLED");
				s.success("good");
			});
		}).map(v -> {
			__logger.info("map CALLED");
			return v + "_mod";
		});
		__logger.info("subscribe CALLED");
		var latch = new CountDownLatch(1);
		mono.subscribe(v -> {
			__logger.info("subscribe RECEIVE result=<{}>", v);
			Assertions.assertEquals("good_mod", v);
			latch.countDown();
		});
		latch.await(1000, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testRepeatSubscribe() throws Exception {
		var count = new AtomicInteger(0);
		var mono = MyMono.create(s -> {
			var id = count.getAndIncrement();
			__logger.info("create CALLED id=<{}>", id);
			s.success(id);
		}).map(v -> {
			return "good_" + v;
		});
		for (int i = 0; i < 3; i++) {
			var id = i;
			mono.subscribe(v -> {
				__logger.info("subscribe RECEIVE result=<{}>", v);
				Assertions.assertEquals("good_" + id, v);
			});
		}
	}

}
