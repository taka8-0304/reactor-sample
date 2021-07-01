package taka8.sample.reactor;

import java.util.function.Consumer;
import java.util.function.Function;

abstract public class MyMono<T> {

	public interface Sink<T> {

		void success(T value);

	}

	abstract public void subscribe(Consumer<? super T> subscriber);

	public static <T> MyMono<T> create(Consumer<Sink<T>> template) {
		return new _SupplierMono<>(template);
	}

	public <V> MyMono<V> map(Function<? super T, ? extends V> mapper) {
		return new _MappingMono<>(this, mapper);
	}

	private static class _MappingMono<I, O> extends MyMono<O> {

		private MyMono<I> _source;

		private Function<? super I, ? extends O> _mapper;

		public _MappingMono(MyMono<I> source, Function<? super I, ? extends O> mapper) {
			super();
			_source = source;
			_mapper = mapper;
		}

		public void subscribe(Consumer<? super O> subscriber) {
			_source.subscribe(i -> {
				var o = _mapper.apply(i);
				subscriber.accept(o);
			});
		}

	}

	private static class _SupplierMono<T> extends MyMono<T> {

		private Consumer<Sink<T>> _template;

		public _SupplierMono(Consumer<Sink<T>> template) {
			super();
			_template = template;
		}

		public void subscribe(Consumer<? super T> subscriber) {
			_template.accept(new Sink<T>() {

				@Override
				public void success(T value) {
					subscriber.accept(value);
				}
			});
		}

	}

}
