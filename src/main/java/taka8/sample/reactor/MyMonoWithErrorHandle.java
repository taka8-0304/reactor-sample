package taka8.sample.reactor;

import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class MyMonoWithErrorHandle<T> {

	private static final Logger __logger = LoggerFactory.getLogger(MyMonoWithErrorHandle.class);

	public interface Sink<T> {

		void success(T value);

		void error(Throwable error);

	}

	public static <T> MyMonoWithErrorHandle<T> create(Consumer<Sink<T>> template) {
		return new _SupplierMono<>(template);
	}

	abstract protected void subscribe(Subscriber<? super T> subscriber);

	public <V> MyMonoWithErrorHandle<V> map(Function<? super T, ? extends V> mapper) {
		return new _MappingMono<>(this, mapper);
	}

	public <V> MyMonoWithErrorHandle<T> onErrorResume(
			Function<? super Throwable, ? extends MyMonoWithErrorHandle<? extends T>> errorHandler) {
		return new _ErrorHandleMono<>(this, errorHandler);
	}

	public void subscribe(Consumer<? super T> subscriber) {
		this.subscribe(new Subscriber<T>() {

			@Override
			public void onSubscribe(Subscription s) {
			}

			@Override
			public void onNext(T t) {
				subscriber.accept(t);
			}

			@Override
			public void onError(Throwable t) {
				__logger.warn("Error occurred.", t);
			}

			@Override
			public void onComplete() {
			}
		});
	}

	private static class _MappingMono<I, O> extends MyMonoWithErrorHandle<O> {

		private MyMonoWithErrorHandle<I> _source;

		private Function<? super I, ? extends O> _mapper;

		public _MappingMono(MyMonoWithErrorHandle<I> source, Function<? super I, ? extends O> mapper) {
			super();
			_source = source;
			_mapper = mapper;
		}

		@Override
		public void subscribe(Subscriber<? super O> subscriber) {
			_source.subscribe(i -> {
				var o = _mapper.apply(i);
				subscriber.onNext(o);
			});
		}

	}

	private static class _SupplierMono<T> extends MyMonoWithErrorHandle<T> {

		private Consumer<Sink<T>> _template;

		public _SupplierMono(Consumer<Sink<T>> template) {
			super();
			_template = template;
		}

		@Override
		public void subscribe(Subscriber<? super T> subscriber) {
			_template.accept(new Sink<T>() {

				@Override
				public void success(T value) {
					subscriber.onNext(value);
				}

				@Override
				public void error(Throwable error) {
					subscriber.onError(error);
				}
			});
		}

	}

	private static class _ErrorHandleMono<T> extends MyMonoWithErrorHandle<T> {

		private MyMonoWithErrorHandle<T> _source;

		private Function<? super Throwable, ? extends MyMonoWithErrorHandle<? extends T>> _errorHandler;

		public _ErrorHandleMono(MyMonoWithErrorHandle<T> source,
				Function<? super Throwable, ? extends MyMonoWithErrorHandle<? extends T>> errorHandler) {
			super();
			_source = source;
			_errorHandler = errorHandler;
		}

		@Override
		public void subscribe(Subscriber<? super T> subscriber) {
			_source.subscribe(new Subscriber<T>() {

				@Override
				public void onSubscribe(Subscription s) {
					subscriber.onSubscribe(s);
				}

				@Override
				public void onNext(T t) {
					subscriber.onNext(t);
				}

				@Override
				public void onError(Throwable t) {
					var mono = _errorHandler.apply(t);
					mono.subscribe(subscriber);
				}

				@Override
				public void onComplete() {
					subscriber.onComplete();
				}
			});
		}

	}

}
