package de.nikos410.kapu_vertretung.security;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nikos410.kapu_vertretung.util.IOUtil;

@Component
public class JSONAuthentificationProvider
    implements AuthenticationProvider {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final static Path LOGINS_PATH = Paths.get("logins.json");
         
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public Authentication authenticate(Authentication authentication) 
            throws AuthenticationException {

        final String username = authentication.getName();
        final String password = authentication.getCredentials().toString();

        if (loginCorrect(username, password)) {
            
            List<GrantedAuthority> grantedAuths = new ArrayList<>();
            final String grantedRole;
            switch (username.toLowerCase()) {
                case "admin": grantedRole = "ROLE_ADMIN"; break;
                case "lehrer": grantedRole = "ROLE_TEACHER"; break;
                default: grantedRole = "ROLE_STUDENT"; break;
            }

            grantedAuths.add(new SimpleGrantedAuthority(grantedRole));
            
            log.info(String.format("User %s logged in. Role: %s", username, grantedRole));
            
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password, grantedAuths);
            return token;
        } else {
            log.info(String.format("Failed login on user %s.", username));
            
            return null;
        }
    }
     
    private boolean loginCorrect(final String inputUserName, final String inputPassword) {
        JSONObject jsonLogin;
        try {
            final String jsonFileContent = IOUtil.readFile(LOGINS_PATH);
            jsonLogin = new JSONObject(jsonFileContent);
        } 
        catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
            
        if (jsonLogin.has(inputUserName)) {
                // Nutzer ist in JSON-Datei enthalten
                
            try {
                final String jsonPasswordHash = jsonLogin.getString(inputUserName);
                                                            
                return passwordEncoder.matches(inputPassword, jsonPasswordHash);
            }
            catch (JSONException e) {
                log.error(e + e.getMessage());
                return false;
            }
        }
        else {
            // Nutzer ist nicht in JSON-Datei einthalten
            return false;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
