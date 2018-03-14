package de.nikos410.kapu_vertretung.configuration;

import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.nikos410.kapu_vertretung.parser.ParserProperties;

@Configuration
public class ParserConfiguration {
	
	@Bean
	public ParserProperties parserProperties() {
		ParserProperties parserProperties = new ParserProperties();
		
		parserProperties.setSplanXlsxPath(Paths.get("plan/xlsx/splan.xlsx"));
		parserProperties.setSplanJsonPath(Paths.get("plan/json/splan.json"));
		parserProperties.setLplanXlsxPath(Paths.get("plan/xlsx/lplan.xlsx"));
		parserProperties.setLplanJsonPath(Paths.get("plan/json/lplan.json"));
		
		return parserProperties;
	}
}
