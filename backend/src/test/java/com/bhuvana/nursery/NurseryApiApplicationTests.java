package com.bhuvana.nursery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that the application context starts.
 *
 * <p>Runs with the {@code test} profile so it uses the dedicated
 * {@code bhuvana_nursery_test} database instead of the development database.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class NurseryApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
