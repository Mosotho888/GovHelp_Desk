package com.loggingsystem.springjwtauth;

import com.loggingsystem.springjwtauth.config.messaging.MailConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MailConfig.class)
class SpringjwtauthApplicationTests {


	@Test
	void contextLoads() {
	}

}
