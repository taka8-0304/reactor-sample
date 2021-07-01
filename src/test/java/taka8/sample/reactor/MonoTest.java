package taka8.sample.reactor;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import reactor.util.retry.Retry;

public class MonoTest {

	private static final Logger __logger = LoggerFactory.getLogger(MonoTest.class);

	@Test
	public void testJust() throws Exception {
		final var expected = "good";
		var mono = Mono.just(expected);
		mono.subscribe(actual -> {
			__logger.info("testJust expected=<{}> actual=<{}>", expected, actual);
			Assertions.assertEquals(expected, actual);
		});
	}

	@Test
	public void testSink() throws Exception {
		final var expected = "good";
		var executor = Executors.newSingleThreadExecutor();
		var mono = Mono.create(s -> {
			executor.execute(() -> {
				s.success(expected);
			});
		});
		var latch = new CountDownLatch(1);
		mono.subscribe(actual -> {
			__logger.info("testJust expected=<{}> actual=<{}>", expected, actual);
			Assertions.assertEquals(expected, actual);
			latch.countDown();
		});
		Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testDefer() throws Exception {
		var deferCallCount = new AtomicInteger(0);
		var expecteds = new ArrayList<String>();
		var mono = Mono.defer(() -> {
			var expected = "good_" + deferCallCount.getAndIncrement();
			expecteds.add(expected);
			return Mono.just(expected);
		});
		for (var expected : expecteds) {
			mono.subscribe(actual -> {
				__logger.info("testDefer expected=<{}> actual=<{}>", expected, actual);
				Assertions.assertEquals(expected, actual);
			});
		}
	}

	@Test
	public void testSubscribeSameThread() throws Exception {
		__logger.info("BEGIN");
		var mono = Mono.create(s -> {
			var value = "good";
			__logger.info("CREATE value=<{}>", value);
			s.success(value);
		});
		mono.subscribe(value -> {
			__logger.info("SUBSCRIBE value=<{}>", value);
		});
		__logger.info("END");
	}

	@Test
	public void testSubscribeRunOnOtherThread() throws Exception {
		__logger.info("BEGIN");
		var latch = new CountDownLatch(1);
		var mono = Mono.create(s -> {
			var value = "good";
			__logger.info("CREATE value=<{}>", value);
			s.success(value);
		}).subscribeOn(Schedulers.single());
		mono.subscribe(value -> {
			__logger.info("SUBSCRIBE value=<{}>", value);
			latch.countDown();
		});
		__logger.info("END");
		latch.await();
	}

	@Test
	public void testSubscribeStartWithOtherThread() throws Exception {
		__logger.info("BEGIN");
		var latch = new CountDownLatch(1);
		var mono = Mono.create(s -> {
			var value = "good";
			Mono.just(value).subscribeOn(Schedulers.single()).subscribe(v -> {
				__logger.info("CREATE value=<{}>", value);
				s.success(v);
			});
		});
		mono.subscribe(value -> {
			__logger.info("SUBSCRIBE value=<{}>", value);
			latch.countDown();
		});
		__logger.info("END");
		latch.await();
	}

	@Test
	public void testSubscribeVoid() {
		AtomicInteger doOnSuccessCalled = new AtomicInteger(0);
		AtomicInteger subscribeCalled = new AtomicInteger(0);
		Mono.create(s -> {
			s.success();
		}).doOnSuccess(v -> {
			doOnSuccessCalled.incrementAndGet();
		}).subscribe(v -> {
			subscribeCalled.incrementAndGet();
		});
		Assertions.assertEquals(1, doOnSuccessCalled.get());
		Assertions.assertEquals(0, subscribeCalled.get());
	}

	@Test
	public void testDeferContextualAndRetry() throws Exception {
		final var retryCountName = "retryCount";
		final var lastErrorName = "lastError";
		Retry customStrategy = Retry.from(companion -> companion.handle((retrySignal, sink) -> {
			Context ctx = sink.currentContext();
			int rl = ctx.getOrDefault(retryCountName, 0);
			sink.next(Context.of(retryCountName, rl + 1, lastErrorName, retrySignal.failure()));
		}));
		final var minRetryCount = 5;
		var expected = "good_" + minRetryCount;
		var mono = Mono.deferContextual(context -> {
			var retryCount = context.getOrDefault(retryCountName, 0);
			if (retryCount < minRetryCount) {
				return Mono.error(
						new IllegalArgumentException("Retry count must be greater than <" + minRetryCount + ">"));
			} else {
				return Mono.just("good_" + retryCount);
			}
		}).retryWhen(customStrategy);
		mono.subscribe(actual -> {
			__logger.info("testDeferContextualAndRetry expected=<{}> actual=<{}>", expected, actual);
			Assertions.assertEquals(expected, actual);
		});
	}

}
