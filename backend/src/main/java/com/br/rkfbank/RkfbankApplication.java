package com.br.rkfbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Classe de entrada. @SpringBootApplication junta tres coisas: @Configuration (esta classe pode declarar beans),
// @EnableAutoConfiguration (o Boot configura sozinho o que acha no classpath: Tomcat, JPA, Flyway, Security...)
// e @ComponentScan (procura @Controller, @Service, @Component... a partir deste pacote, com.br.rkfbank).
@SpringBootApplication
public class RkfbankApplication {

	public static void main(String[] args) {
		// Ponto de entrada da JVM para subir toda a aplicacao.
		SpringApplication.run(RkfbankApplication.class, args);
	}

}
