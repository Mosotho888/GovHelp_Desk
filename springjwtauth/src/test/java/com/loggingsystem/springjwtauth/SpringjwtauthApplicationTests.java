package com.loggingsystem.springjwtauth;

import com.loggingsystem.springjwtauth.config.messaging.MailConfig;
import com.loggingsystem.springjwtauth.config.messaging.MailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MailConfig.class)
@EnableConfigurationProperties(MailProperties.class)
class SpringjwtauthApplicationTests {


	@Test
	void contextLoads() {
	}

}
