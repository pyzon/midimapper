package kristofkallo.midimapper;

import org.xml.sax.SAXException;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The application class.
 * It has a tray menu. It is initialized in the constructor, and torn down
 * in the quit function. The tray icon is used for user actions, and for
 * displaying error or warning messages.
 *
 * It has MIDI devices for two-way communication.
 * It establishes MIDI connections, registering receivers.
 */
public class App {
    static final String APP_NAME = "M-400 MIDI Mapper";

    private final TrayMenu trayMenu;

    private MidiDevice m400In; // M-400 console -> this program
    private MidiDevice loopMidiIn; // this program -> loopMidi (-> DAW)
    private MidiDevice loopMidiOut; // (DAW ->) loopMidi -> this program
    private MidiDevice m400Out; // this program -> M-400 console

    private MidiMap midiMap;

    public App() throws FileNotFoundException, AWTException {
        trayMenu = new TrayMenu(this);
        loadConfig();
        connectDevices();
    }
    public void loadConfig() {
        try {
            midiMap = new MidiMap("resources/map.xml");
        } catch (ParserConfigurationException e) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "Parser configuration error: " + e.getLocalizedMessage(), TrayIcon.MessageType.ERROR);
            e.printStackTrace();
        } catch (IOException e) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "Error reading map.xml: " + e.getLocalizedMessage(), TrayIcon.MessageType.ERROR);
            e.printStackTrace();
        } catch (SAXException e) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "Error parsing map.xml: " + e.getLocalizedMessage(), TrayIcon.MessageType.ERROR);
            e.printStackTrace();
        }
    }
    public void connectDevices() {
        closeDevices();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            MidiDevice device;
            try {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException e) {
                trayMenu.getTrayIcon().displayMessage(APP_NAME, "MIDI error: " + e.getLocalizedMessage(), TrayIcon.MessageType.ERROR);
                return;
            }
//            System.out.println(info.getName());

            if (info.getName().contains("RSS M-400") && device.getMaxTransmitters() != 0) {
                m400In = device;
                try {
                    m400In.open();
                } catch (MidiUnavailableException e) {
                    trayMenu.getTrayIcon().displayMessage(APP_NAME, "Could not open M-400 MIDI In port.", TrayIcon.MessageType.ERROR);
                    return;
                }
            }
            if (info.getName().contains("RSS M-400") && device.getMaxReceivers() != 0) {
                m400Out = device;
                try {
                    m400Out.open();
                } catch (MidiUnavailableException e) {
                    trayMenu.getTrayIcon().displayMessage(APP_NAME, "Could not open M-400 MIDI Out port.", TrayIcon.MessageType.ERROR);
                    return;
                }
            }
            if (info.getName().equals("loopMidiIn") && device.getMaxReceivers() != 0) {
                loopMidiIn = device;
                try {
                    loopMidiIn.open();
                } catch (MidiUnavailableException e) {
                    trayMenu.getTrayIcon().displayMessage(APP_NAME, "Could not open loopMidiIn.", TrayIcon.MessageType.ERROR);
                    return;
                }
            }
            if (info.getName().equals("loopMidiOut") && device.getMaxTransmitters() != 0) {
                loopMidiOut = device;
                try {
                    loopMidiOut.open();
                } catch (MidiUnavailableException e) {
                    trayMenu.getTrayIcon().displayMessage(APP_NAME, "Could not open loopMidiOut.", TrayIcon.MessageType.ERROR);
                    return;
                }
            }
        }
        // Error checking
        if (m400In == null) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "M-400 (In port) device not found. Is the console plugged in and running?", TrayIcon.MessageType.ERROR);
            return;
        }
        if (m400Out == null) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "M-400 (Out port) device not found. Is the console plugged in and running?", TrayIcon.MessageType.ERROR);
            return;
        }
        if (loopMidiIn == null) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "loopMidiIn device not found. Is loopMIDI running?", TrayIcon.MessageType.ERROR);
            return;
        }
        if (loopMidiOut == null) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "loopMidiOut device not found. Is loopMIDI running?", TrayIcon.MessageType.ERROR);
            return;
        }
        // Checks for isOpen() are not necessary
        // Get receivers
        Receiver loopMidiReceiver = null;
        try {
            loopMidiReceiver = loopMidiIn.getReceiver();
        } catch (MidiUnavailableException e) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "loopMidi receiver could not be retrieved. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
            e.printStackTrace();
            return;
        }
        Receiver m400Receiver = null;
        try {
            m400Receiver = m400Out.getReceiver();
        } catch (MidiUnavailableException e) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "M-400 receiver could not be retrieved. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
            e.printStackTrace();
            return;
        }
        // Set receivers on the transmitters
        try {
            m400In.getTransmitter().setReceiver(new M400Receiver(loopMidiReceiver, midiMap));
        } catch (MidiUnavailableException e) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "m400 transmitter could not be retrieved. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
            e.printStackTrace();
            return;
        }
//        try {
//            loopMidiOut.getTransmitter().setReceiver(new LoopMidiReceiver(m400Receiver));
//        } catch (MidiUnavailableException e) {
//            trayMenu.getTrayIcon().displayMessage(APP_NAME, "m400 transmitter could not be retrieved. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
//            e.printStackTrace();
//            return;
//        }
    }
    private void closeDevices() {
        if (m400In != null) {
            m400In.close();
            m400In = null;
        }
        if (m400Out != null) {
            m400Out.close();
            m400Out = null;
        }
        if (loopMidiIn != null) {
            loopMidiIn.close();
            loopMidiIn = null;
        }
        if (loopMidiOut != null) {
            loopMidiOut.close();
            loopMidiOut = null;
        }
    }
    public void quit() {
        closeDevices();
        trayMenu.destroy();
        System.exit(0);
    }

    public TrayMenu getTrayMenu() {
        return trayMenu;
    }
}
