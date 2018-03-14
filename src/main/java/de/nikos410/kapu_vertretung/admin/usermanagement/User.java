package de.nikos410.kapu_vertretung.admin.usermanagement;

/**
 * Enthält die Änderungen an einem Nutzer über die Admin-Seite
 * Wird an das HTML-Template übergeben, wird ausgefüllt
 */
public class User {
    /**
     * Der Name des Nutzers
     */
    private String name;
    /**
     * Das Passwort des Nutzers
     */
    private String password;

    /**
     * Gibt den Namen des Nutzers zurück
     * @return Der Name des Nutzers
     */
    public String getName() {
        return name;
    }

    /**
     * Namen des Nutzers setzen
     * @param name der Name der gesetzt werden soll
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gibt das Passwort des Nutzers zurück
     * @return das Passwort des Nutzers
     */
    public String getPassword() {
        return password;
    }

    /**
     * Passwort des Nutzers setzen
     * @param password das Passwort das gesetzt werden soll
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
