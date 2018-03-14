package de.nikos410.kapu_vertretung.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class IOUtil {

    private final static Logger log = LoggerFactory.getLogger(IOUtil.class);

	public static String readFile(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        catch (IOException e){
            log.error(e.getMessage(), e.getCause());
            return null;
        }
    }

    public static Path writeToFile(Path path, String content) {
        try {
            return Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            log.error(e + e.getMessage());
            return null;
        }
    }
}
