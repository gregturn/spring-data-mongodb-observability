package com.example.springdatamongodbobservability;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.observability.EnableMongoObservability;
import org.springframework.data.mongodb.observability.MongoMetricsConfigurationHelper;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;
import org.springframework.data.mongodb.observability.MongoTracingObservationHandler;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import com.mongodb.ConnectionString;

@SpringBootApplication
@EnableMongoObservability
public class SpringDataMongodbObservabilityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringDataMongodbObservabilityApplication.class, args);
	}

	// Setting up MongoDB

	@Bean(initMethod = "start", destroyMethod = "stop")
	MongoDBContainer mongoDBContainer() {
		return new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"));
	}

	@Bean
	CommandLineRunner initDatabase(EmployeeRepository repository) {
		return args -> {
			repository.save(new Employee("Frodo", "ring bearer"));
			repository.save(new Employee("Bilbo", "burglar"));
		};
	}

	// Configuring MongoDB + Micrometer

	@Bean
	MongoClientSettingsBuilderCustomizer mongoMetricsSynchronousContextProvider(Tracer tracer,
			ObservationRegistry registry) {
		return (clientSettingsBuilder) -> {
			clientSettingsBuilder.contextProvider( //
					MongoMetricsConfigurationHelper.synchronousContextProvider(tracer, registry));
		};
	}

	@Bean
	MongoClientSettingsBuilderCustomizer mongoObservationCommandListenerCustomizer(MongoDBContainer mongoDBContainer,
			MongoObservationCommandListener commandListener) {
		return (clientSettingsBuilder) -> clientSettingsBuilder //
				.applyConnectionString(new ConnectionString(mongoDBContainer.getConnectionString())) //
				.addCommandListener(commandListener);
	}

	@Bean
	ObservationRegistryCustomizer<ObservationRegistry> mongoTracingHandlerCustomizer(
			MongoTracingObservationHandler handler) {
		return handler::register;
	}

}
