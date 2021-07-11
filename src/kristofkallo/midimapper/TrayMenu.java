package kristofkallo.midimapper;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.net.URL;

public class TrayMenu {
    public static final String TRAY_ICON_FILENAME = "icon.png";

    private final App app;
    private final TrayIcon trayIcon;
    //private final MenuItem m400MenuItem;
    //private final MenuItem loopMidiMenuItem;

    public TrayMenu(App app) throws FileNotFoundException, AWTException{
        this.app = app;
        PopupMenu menu = new PopupMenu();
        MenuItem reconnectMenuItem = new MenuItem("Reconnect");
        reconnectMenuItem.addActionListener(e -> this.app.connectDevices());
        menu.add(reconnectMenuItem);
        //Menu devicesMenu = new Menu("Devices");
        //m400MenuItem = new MenuItem();
        //m400MenuItem.setEnabled(false);
        //loopMidiMenuItem = new MenuItem();
        //loopMidiMenuItem.setEnabled(false);
        //menu.add(m400MenuItem);
        //menu.add(loopMidiMenuItem);
        MenuItem quitMenuItem = new MenuItem("Quit");
        quitMenuItem.addActionListener(e -> this.app.quit());
        menu.add(quitMenuItem);
        Image icon = createIcon();
        trayIcon = new TrayIcon(icon, App.APP_NAME, menu);
        SystemTray.getSystemTray().add(trayIcon);
    }
    private Image createIcon() throws FileNotFoundException {
        URL imageURL = Main.class.getResource(TRAY_ICON_FILENAME);
        if (imageURL == null) {
            throw new FileNotFoundException(TRAY_ICON_FILENAME + " not found.");
        }
        return (new ImageIcon(imageURL)).getImage();
    }
    public void destroy() {
        SystemTray.getSystemTray().remove(trayIcon);
    }
//    public void setM400MenuItemConnected(boolean connected) {
//        m400MenuItem.setLabel(connected ? "M-400 connected" : "M-400 disconnected");
//    }
//    public void setLoopMidiMenuItemConnected(boolean connected) {
//        loopMidiMenuItem.setLabel(connected ? "loopMidi connected" : "loopMidi disconnected");
//    }

    public TrayIcon getTrayIcon() {
        return trayIcon;
    }
}
