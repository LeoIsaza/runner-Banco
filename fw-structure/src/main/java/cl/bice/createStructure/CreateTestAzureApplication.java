/**
 * 
 */
package cl.bice.createStructure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import cl.bice.createStructure.service.CreateTestService;

@SpringBootApplication
public class CreateTestAzureApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreateTestAzureApplication.class, args);
	}
	

	@Autowired
	private CreateTestService createTestService;

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> createTestService.create();

	}

}
