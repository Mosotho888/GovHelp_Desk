package com.loggingsystem.springjwtauth;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
class SpringjwtauthApplicationTests {


	@Mock
	private JavaMailSender javaMailSender;

	@Test
	void contextLoads() {
	}

}
