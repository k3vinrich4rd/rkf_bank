package com.br.rkfbank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.times;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

@SpringBootTest
class RkfbankApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void deveInvocarMainDaAplicacao() {
		try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
			RkfbankApplication.main(new String[]{});
			mocked.verify(() -> SpringApplication.run(RkfbankApplication.class, new String[]{}), times(1));
		}
	}

}
