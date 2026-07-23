package io.github.bsidedevs.api_review;

import org.springframework.boot.SpringApplication;

public class TestApiReviewApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiReviewApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
