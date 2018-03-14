package de.nikos410.kapu_vertretung.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.nikos410.kapu_vertretung.parser.Parser;
import de.nikos410.kapu_vertretung.parser.ParserProperties;
import de.nikos410.kapu_vertretung.parser.SplanParser;
import de.nikos410.kapu_vertretung.parser.LplanParser;
import de.nikos410.kapu_vertretung.storage.StorageFileNotFoundException;
import de.nikos410.kapu_vertretung.storage.StorageService;
import de.nikos410.kapu_vertretung.util.IOUtil;

import javax.annotation.PostConstruct;

/**
 * Controller für Datei-Uploads
 */
@Controller
public class FileUploadController {

    /**
     *
     */
    private final StorageService storageService;
    private final ParserProperties parserProperties;

    @Autowired
    public FileUploadController(StorageService storageService, ParserProperties parserProperties) {
        this.storageService = storageService;
        this.parserProperties = parserProperties;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/upload/splan")
    public String handleSplanUpload(@RequestParam("file") MultipartFile uploadedFile, Model model, RedirectAttributes redirectAttributes) {
    	// Datei hochladen
        final Path tempPath = storageService.store(uploadedFile);
        
        // Hochgeladene Datei in richtigen Ordner verschieben
        final Path storedPath = parserProperties.getSplanXlsxPath();
        
        try {
			Files.move(tempPath, storedPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
        
        // Datei Parsen
        Parser parser = new SplanParser(storedPath);
        final String jsonContent;
        try {
        	jsonContent = parser.parseToJSON();
        }
        catch (JSONException e) {
        	redirectAttributes.addFlashAttribute("message", "Interner Fehler!");
        	redirectAttributes.addFlashAttribute("errorMsg", e.toString());
        	return "redirect:/admin";
        }

        // Ergebnis in JSON Datei schreiben
        final Path jsonPath = parserProperties.getSplanJsonPath();
        IOUtil.writeToFile(jsonPath, jsonContent);
        
        redirectAttributes.addFlashAttribute("message", "Plan hochgeladen.");
        redirectAttributes.addFlashAttribute("uploadedFile", true);
        
        return "redirect:/admin";
    }
    
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/upload/lplan")
    public String handleLplanUpload(@RequestParam("file") MultipartFile uploadedFile, Model model, RedirectAttributes redirectAttributes) {
        // Datei hochladen
        final Path tempPath = storageService.store(uploadedFile);

        // Hochgeladene Datei in richtigen Ordner verschieben
        final Path storedPath = parserProperties.getLplanXlsxPath();

        try {
            Files.move(tempPath, storedPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        // Datei Parsen
        Parser parser = new LplanParser(storedPath);
        final String jsonContent;
        try {
        	jsonContent = parser.parseToJSON();
        }
        catch (JSONException e) {
        	redirectAttributes.addFlashAttribute("message", "Interner Fehler!");
        	redirectAttributes.addFlashAttribute("errorMsg", e.toString());
        	return "redirect:/admin";
        }

        // Ergebnis in JSON Datei schreiben
        final Path jsonPath = parserProperties.getLplanJsonPath();
        IOUtil.writeToFile(jsonPath, jsonContent);
        
        redirectAttributes.addFlashAttribute("message", "Plan hochgeladen.");
        redirectAttributes.addFlashAttribute("uploadedFile", true);
        
        return "redirect:/admin";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }
}