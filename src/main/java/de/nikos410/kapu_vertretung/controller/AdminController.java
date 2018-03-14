package de.nikos410.kapu_vertretung.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.nikos410.kapu_vertretung.util.IOUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.nikos410.kapu_vertretung.admin.usermanagement.User;

/**
 * Controller für den Admin-Bereich
 */
@Controller
public class AdminController {

    /**
     * Datei, welche die Bcypt-Hashes der Benutzerlogins enthält
     */
    private final static Path LOGINS_PATH = Paths.get("logins.json");

    /**
     * Logger um Informationen und Fehlermeldungen auszugeben
     */
    private final static Logger log = LoggerFactory.getLogger(IOUtil.class);


    /**
     * Bcrypt-Encoder
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Aufruf des Admin-Bereiches
     *
     * @param model wird injected um Attribute anfügen zu können
     * @return Template das angezeigt werden soll -> Hier: admin.html
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")  // Nutzer muss als Admin eingeloggt sein um diesen Bereich sehen zu können
    @GetMapping("/admin")   // GET-Aufrufe auf /admin werden durch diese Methode bearbeitet
    public String adminArea(Model model) {
        try {
            // Liste der Nutzer die bearbeitet werden können
            model.addAttribute("userList", makeUserList());
            // Objekt um Nutzer zu bearbeiten
            model.addAttribute("editUser", new User());
        }
        catch (Exception e) {
            // Fehler auf der Seite anzeigen
            model.addAttribute("message", "Interner Fehler!");
            model.addAttribute("subMessage", e.toString());
            // Fehler loggen
            log.error(e.getMessage(), e.getCause());
        }

        // Template admin.html anzeigen
        return "admin";
    }

    /**
     * POST-Anfrage mit einer Nutzeränderung
     *
     * @param editUser Objekt das die Änderungen am Nutzer enthält
     * @param userCreate gibt an ob der Nutzer neu erstellt wurde
     * @param pwChange gibt an ob das Passwort des Nutzers geändert wurde
     * @param userDelete gibt an ob der Nutzer gelöscht wurde
     * @param redirectAttributes wird injected um Attribute anfügen zu können
     * @return Template das angezeigt werden soll -> Hier: Weiterleitung auf /admin
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/userEdit")
    public String userEdit(@ModelAttribute User editUser,
                           @RequestParam(value = "userCreate", required = false) String userCreate,
                           @RequestParam(value = "pwChange", required = false) String pwChange,
                           @RequestParam(value = "userDelete", required = false) String userDelete,
            RedirectAttributes redirectAttributes ){
        try {
            // Alle Logins einlesen
            final String loginJsonContent = new String(Files.readAllBytes(LOGINS_PATH), StandardCharsets.UTF_8); 
            final JSONObject loginJson = new JSONObject(loginJsonContent);

            // Name des Benutzers
            final String name = editUser.getName();

            if (userCreate != null) {
                // Nutzer erstellen wurde ausgewählt

                // Passwort des Benutzers
                final String password = editUser.getPassword();

                // Bcrypt-Hash des Passwortes
                final String passwordHash = passwordEncoder.encode(password);
                
                if (loginJson.has(name)) {
                    // Nutzer existiert bereits

                    // Fehlermeldung als Attribut anfügen
                    redirectAttributes.addFlashAttribute("message", "Fehler!");
                    redirectAttributes.addFlashAttribute("errorMsg", "Nutzer existiert bereits!");
                }
                else {
                    // Nutzer kann erstellt werden

                    // Login-Daten in JSON-Datei hinzufügen
                    loginJson.put(name, passwordHash);
                    // JSON-Datei speichern
                    saveJSON(loginJson);
                }
            }
            else if(pwChange != null) {
                // Passwortänderung wurde ausgewählt

                // Neues Passwort
                final String newPassword = editUser.getPassword();

                // Bcrypt-Hash des neuen Passwortes
                final String newPasswordHash = passwordEncoder.encode(newPassword);

                // Login-Daten in JSON-Datei aktualiseren
                if (loginJson.has(name)) {
                    loginJson.remove(name);
                }
                loginJson.put(name, newPasswordHash);
                // JSON-Datei speichern
                saveJSON(loginJson);

                // Bestätigung als Attribut anfügen
                redirectAttributes.addFlashAttribute("message", "Passwort erfolgreich geändert!");
            }
            else if (userDelete != null) {
                // Nutzer löschen wurde ausgewählt

                if (loginJson.has(name)) {
                    // Nutzer aus JSON-Datei entfernen
                    loginJson.remove(name);
                    // JSON-Datei speichern
                    saveJSON(loginJson);

                    // Bestätigung als Attribut anfügen
                    redirectAttributes.addFlashAttribute("message", "Nutzer erfolgreich gelöscht!");
                }
                else {
                    // Fehlermeldung als Attribut anfügen
                    redirectAttributes.addFlashAttribute("message", "Fehler!");
                    redirectAttributes.addFlashAttribute("errorMsg",
                            "Nutzer der gelöscht werden soll ist in JSON-Datei nicht vorhanden. Dies sollte nicht passieren.");
                }
            }
            
        }
        catch(Exception e) {
            // Fehlermeldung als Attribut anfügen
            redirectAttributes.addAttribute("message", "Interner Fehler!");
            redirectAttributes.addAttribute("errorMsg", e.toString());
            log.error(e.getMessage(), e);
        }

        // Dem Template mitteilen dass ein Nutzer bearbeitet wurde -> Nutzerverwaltung soll aufgeklappt sein
        redirectAttributes.addFlashAttribute("editedUser", true);

        // Umleitung auf /admin
        return "redirect:/admin";
    }

    /**
     * Liste mit den Namen aller verfügbarer Nutzer erstellen
     *
     * @return Liste mit den Namen aller verfügbarer Nutzer
     * @throws Exception Fehler der beim Lesen der Datei auftreten könnte, wird hochgereicht um als Fehlermeldung
     * auf der Seite angezeigt werden zu können
     */
    private List<String> makeUserList() throws Exception {
        // Alle Logins einlesen
        final String loginJsonContent = new String(Files.readAllBytes(LOGINS_PATH), StandardCharsets.UTF_8);
        final JSONObject loginJson = new JSONObject(loginJsonContent);

        // Liste die die Namen enthalten soll
        List<String> userList = new LinkedList<>(); 

        // Wird genutzt um über alle Keys der JSON-Datei (-> Benutzernamen) zu iterieren
        final Iterator<String> userIterator = loginJson.keys();
        // Über alle Keys der JSON-Datei iterieren
        while (userIterator.hasNext()) {
            // Aktueller Key
            final String user = userIterator.next();
            
            // Lehrer und Admin können nicht geändert/gelöscht werden
            if (!user.equalsIgnoreCase("admin") && !user.equalsIgnoreCase("lehrer")) {
                // Nutzer zur Liste hinzufügen
                userList.add(user);
            }
        }

        // Liste alphabetisch sortieren
        Collections.sort(userList);

        // fertige Liste zurückgeben
        return userList;
    }

    /**
     * JSON-Datei nach einer Änderung abspeichern
     *
     * @param json Das JSONObject das in die Datei geschrieben werden soll
     * @throws Exception Fehler der beim Schreiben der Datei auftreten könnte, wird hochgereicht um als Fehlermeldung
     * auf der Seite angezeigt werden zu können
     */
    private void saveJSON(JSONObject json) throws Exception{
        // JSONObject in einen String umwandeln und einrücken
        final String jsonOutput = json.toString(4);
        // String in eine Datei schreiben
        Files.write(LOGINS_PATH, jsonOutput.getBytes(StandardCharsets.UTF_8));
    }
}
