package de.nikos410.kapu_vertretung.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import de.nikos410.kapu_vertretung.parser.ParserProperties;

@Controller
public class SplanController {
	
	private final ParserProperties parserProperties;
	
	@Autowired
    public SplanController(ParserProperties parserProperties) {
        this.parserProperties = parserProperties;
    }
	
	@PreAuthorize("hasRole('ROLE_STUDENT')")
	@GetMapping("/splan")
	public String splan(Model model, UsernamePasswordAuthenticationToken token) {
		final String user = token.getName();
		
		// Nutzernamen übergeben
		model.addAttribute("user", user);
		
		try {
			final String sPlanJsonContent = new String(Files.readAllBytes(this.parserProperties.getSplanJsonPath()), StandardCharsets.UTF_8); 
			
			final JSONObject sPlanJson = new JSONObject(sPlanJsonContent);

			if (!sPlanJson.has(user)) {
				// Nutzer ist nicht in JSON Datei enthalten -> keine Vertretungen
				model.addAttribute("message", "Für dich liegen keine Vertretungen vor :/");
				return "splan";
			}
			
			// Liste die die Vertretungen enthält, jede Hashmap entspricht einer Vertretung
			List< HashMap<String, String> > vList = new LinkedList<>();
			
			// Alle Vertretungen die für den Nutzer relevant sind
			final JSONArray userPlanJson = sPlanJson.getJSONArray(user);
	
			// Über alle Vertretungen des Nutzers iterieren
			for (int v = 0; v < userPlanJson.length(); v++) {
				// Array entspricht einer Vertretung
				final JSONArray vArray = userPlanJson.getJSONArray(v);
				// Map entspricht einer Vertretung
				final LinkedHashMap<String, String> map = new LinkedHashMap<>();
				
				// Über alle Felder dieser Vertretung iterieren
				for (int i = 0; i < vArray.length(); i++) {
					final JSONObject vObject = vArray.getJSONObject(i);
									
					map.put(vObject.getString("key"), vObject.getString("value"));
				}
				vList.add(map);
			}
			
			model.addAttribute("vList", vList);
			return "splan";
		}
		catch (Exception e) {
			model.addAttribute("message", "Interner Fehler!");
			model.addAttribute("errorMsg", e.toString());
			e.printStackTrace();
			return "splan";
		}
	}
}
