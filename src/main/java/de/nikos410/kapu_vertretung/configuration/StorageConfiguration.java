package de.nikos410.kapu_vertretung.configuration;

import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.nikos410.kapu_vertretung.storage.StorageProperties;

@Configuration
public class StorageConfiguration {
	
	@Bean
	public StorageProperties storageProperties() {
		StorageProperties properties = new StorageProperties();
		properties.setLocation(Paths.get("upload"));
		return properties;
	}
}
