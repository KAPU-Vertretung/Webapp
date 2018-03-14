package de.nikos410.kapu_vertretung.parser;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("parser")
public class ParserProperties {
	
	private Path splanXlsxPath;
	private Path splanJsonPath;
	
	private Path lplanXlsxPath;
	private Path lplanJsonPath;

	
	public Path getSplanXlsxPath() {
		return splanXlsxPath;
	}
	public Path getSplanJsonPath() {
		return splanJsonPath;
	}
	
	public Path getLplanXlsxPath() {
		return lplanXlsxPath;
	}
	public Path getLplanJsonPath() {
		return lplanJsonPath;
	}
	
	
	public void setSplanXlsxPath(Path path) {
		this.splanXlsxPath = path;
	}
	public void setSplanJsonPath(Path path) {
		this.splanJsonPath = path;
	}
	
	public void setLplanXlsxPath(Path path) {
		this.lplanXlsxPath = path;
	}
	public void setLplanJsonPath(Path path) {
		this.lplanJsonPath = path;
	}
}
