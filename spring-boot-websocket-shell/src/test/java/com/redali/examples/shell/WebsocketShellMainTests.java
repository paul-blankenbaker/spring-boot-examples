package com.redali.examples.shell;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

// IMPORTANT: You need to switch off interactive mode when testing
// you can do it individually like this, or by adding the following
// line to an applications.properties file in your test resources:
//
// spring.shell.interactive.enabled=false
@SpringBootTest(properties = "spring.shell.interactive.enabled=false")
class WebsocketShellMainTests {
	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		Assertions.assertNotNull(applicationContext);
	}

}
