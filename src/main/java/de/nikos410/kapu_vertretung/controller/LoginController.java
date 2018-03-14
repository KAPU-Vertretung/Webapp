package de.nikos410.kapu_vertretung.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.nikos410.kapu_vertretung.security.JSONAuthentificationProvider;

/**
 * Controller für die Login-Seite
 */
@Controller
public class LoginController {

	/**
	 * Authentifiziert Nutzer
	 */
	private JSONAuthentificationProvider jsonAuthentificationProvider;

	/**
	 * @param jsonAuthentificationProvider wird injected
	 */
	@Autowired
    public LoginController(JSONAuthentificationProvider jsonAuthentificationProvider) {
        this.jsonAuthentificationProvider = jsonAuthentificationProvider;
    }


	/**
	 * Weiterleitung auf Login-Seite
	 *
	 * @return Template das angezeigt werden soll -> Hier: Weiterleitung auf /login
	 */
	@GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

	/**
	 * Aufruf der Login-Seite
	 *
	 * @param logout Gibt an ob der Nutzer sich zuvor ausgeloggt hat
	 * @param invalid Gibt an ob die Anmeldedaten zuvor ungültig waren
	 * @param model Wird injected um Message als Attriburt anfügen zu können
	 * @return Template das angezeigt werden soll -> Hier: login.html
	 */
	@GetMapping("/login")
    public String loginForm(@RequestParam(value="logout", required=false) String logout, 
            @RequestParam(value="invalid", required=false) String invalid, Model model) {
        
        if (logout != null) {
			// Logout
            model.addAttribute("message", "Du wurdest erfolgreich ausgeloggt!");
        }
        else if (invalid != null) {
			// Ungültige Anmeldedaten
            model.addAttribute("message", "Ungültige Anmeldedaten!");
        }

		// Template login.html
        return "login";
    }

	/**
	 * Aufruf von /login/success (URL bei erfolgreichem Login, konfiguriert in configuration.SecurityConfiguration)
	 * Unterscheidung von Benutzernamen und Weiterleitung auf entsprechenden Bereich
	 *
	 * @param token Login-Daten des Nutzers
	 * @return URL auf die weitergeleitet werden soll
	 */
	@GetMapping("/login/success")
    public String loginRedirect(UsernamePasswordAuthenticationToken token) {
		// Name des Nutzers
        final String username = token.getName();
        
        switch (username.toLowerCase()) {
        	// Nutzer ist als Admin eingeloggt
            case "admin": return "redirect:/admin";
            // Nutzer ist als Lehrer eingeloggt
            case "lehrer": return "redirect:/lplan";
            // Nutzer ist als Schüler eingeloggt
            default: return "redirect:/splan";
        }
    }

	/**
	 * Aufruf von /logout
	 * Anmeldetoken resetten und auf Login-Seite zurückleiten
	 *
	 * @param token Anmeldetoken
	 * @return Template das angezeigt werden soll -> Hier: Weiterleitung auf /login
	 */
	@GetMapping("/logout")
    public String logout(UsernamePasswordAuthenticationToken token) {
		// Anmeldedaten leren
        token.eraseCredentials();
        // Weiterleitung auf Login-Seite
        return "redirect:/login";
    }
}
