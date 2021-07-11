package kristofkallo.midimapper;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;

/***
 * The entry point of the application.
 * The JPanel is there for displaying application-level error,
 * such as error in creating a tray icon.
 */
public class Main {

    static App app;
    static private final JPanel panel = new JPanel();

    public static void main(String[] args) {
        panel.setVisible(false);
        try {
            app = new App();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(panel, "Tray icon error: " + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            app.quit();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(panel, "File not found: " + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            app.quit();
        } /*catch (MidiUnavailableException e) {
            JOptionPane.showMessageDialog(panel, "MIDI unavailable: " + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            app.quit();
        }*/
    }
}
