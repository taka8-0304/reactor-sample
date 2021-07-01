package taka8.sample.reactor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MyMonoWithErrorHandleTest {

	@Test
	public void testOnErrorResume() {
		var mono = MyMonoWithErrorHandle.create(s -> {
			s.error(new IllegalArgumentException("Invalid call."));
		}).onErrorResume(th -> {
			return MyMonoWithErrorHandle.create(s -> {
				s.success("good");
			});
		});
		mono.subscribe(v->{
			Assertions.assertEquals("good", v);
		});
	}

}
