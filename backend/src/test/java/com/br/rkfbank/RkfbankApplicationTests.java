package com.br.rkfbank;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.times;


@SpringBootTest
// Perfil test explicito: roda igual no IDE e no Maven (H2, sem seed, sem o .env de quem roda).
@ActiveProfiles("test")
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
