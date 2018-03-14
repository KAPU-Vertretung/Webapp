package de.nikos410.kapu_vertretung.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.nikos410.kapu_vertretung.parser.ParserProperties;

@Controller
public class LplanController {
    
private final ParserProperties parserProperties;
    
    @Autowired
    public LplanController(ParserProperties parserProperties) {
        this.parserProperties = parserProperties;
    }

    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @GetMapping("/lplan")
    public String lplan(Model model) {
        try {
            final String lPlanJsonContent = new String(Files.readAllBytes(this.parserProperties.getLplanJsonPath()), StandardCharsets.UTF_8); 
            final JSONObject lPlanJson = new JSONObject(lPlanJsonContent);
            
            List<String> teacherList = makeTeacherList(lPlanJson);
            if (teacherList != null) {
                model.addAttribute("teacherList", teacherList);
            }
            else {
                model.addAttribute("message", "Es liegen keine Vertretungen vor!");
            }
        
            return "lplan";
        }
        catch (Exception e) {
            model.addAttribute("message", "Interner Fehler!");
            model.addAttribute("errorMsg", e.toString());
            return "lplan";
        }
    }
    
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @PostMapping("/lplan")
    public String lplanSubmit(Model model,  @RequestParam(value = "teacherSelect", required = false) String selectedTeacher) {
        try {
            final String lPlanJsonContent = new String(Files.readAllBytes(this.parserProperties.getLplanJsonPath()), StandardCharsets.UTF_8); 
            final JSONObject lPlanJson = new JSONObject(lPlanJsonContent);
            
            List<String> teacherList = makeTeacherList(lPlanJson);
            if (teacherList != null) {
                model.addAttribute("teacherList", teacherList);
            }
            else {
                model.addAttribute("message", "Es liegen keine Vertretungen vor!");
            }
        
            // kein Lehrer angegeben
            if (selectedTeacher == null) {
                return "lplan";
            }
            
            model.addAttribute("selectedTeacher", selectedTeacher);
            
            // Liste die die Vertretungen enthält, jede Hashmap entspricht einer Vertretung
            List< HashMap<String, String> > vList = new LinkedList<>();
                        
            // Alle Vertretungen die für den Nutzer relevant sind
            final JSONArray userPlanJson = lPlanJson.getJSONArray(selectedTeacher);
                
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
            
            return "lplan";
        }
        catch (Exception e) {
            model.addAttribute("message", "Interner Fehler!");
            model.addAttribute("errorMsg", e.toString());
            e.printStackTrace();
            return "lplan";
        }
    }
    
    private static List<String> makeTeacherList(final JSONObject lplanJson) throws Exception {
        List<String> teacherList = new LinkedList<>(); 
        
        final Iterator<String> keyIterator = lplanJson.keys();
        if (!keyIterator.hasNext()) {
            // Keine vertretungen für Lehrer
            return null;
        }
        
        while (keyIterator.hasNext()) {
            teacherList.add(keyIterator.next());
        }

        Collections.sort(teacherList);
        return teacherList;
    }
}
