package io.github.bsidedevs.api_review;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ApiReviewApplicationTests {

	@Test
	void contextLoads() {
	}

}
