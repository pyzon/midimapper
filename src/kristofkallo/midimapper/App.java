package kristofkallo.midimapper;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import java.awt.*;
import java.io.FileNotFoundException;

/**
 * The application class.
 * It has a tray menu. It is initialized in the constructor, and torn down
 * in the quit function. The tray icon is used for user actions, and for
 * displaying error or warning messages.
 * It has a bunch of MidiDevices in a hashmap. The reason for this is that we
 * already have like 10 and in the future there may be even more.
 *
 * A MidiDevice is what the OS sees when you plug in a physical device.
 * Usually each device has two instances, for an IN and an OUT port.
 * An IN port is the one that sends data from the device to the OS, and it is
 * represented as a MidiDevice that can have a Transmitter. Remember this.
 * On the other hand, an OUT port is the one that sends data from the OS back
 * to the device. This one can have a Receiver.
 *
 *
 * It establishes MIDI connections, registering receivers
 */
public class App {
    static final String APP_NAME = "M-400 MIDI Mapper";

    private final TrayMenu trayMenu;
    private MidiDevice m400In; // M-400 console -> this program
    private MidiDevice loopMidiIn; // this program -> loopMidi (-> DAW)
    private MidiDevice loopMidiOut; // (DAW ->) loopMidi -> this program
    private MidiDevice m400Out; // this program -> M-400 console
//    private Receiver loopMidiOutReceiver;
//    private final JPanel panel;

    public App() throws FileNotFoundException, AWTException {
//        panel = new JPanel();
//        panel.setVisible(false);
        trayMenu = new TrayMenu(this);
        connectDevices();
//        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
//        scheduledThreadPool.scheduleAtFixedRate(() -> checkConnection(), 0, 1, TimeUnit.SECONDS);
    }
    public void connectDevices(){
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
            m400In.getTransmitter().setReceiver(new M400Receiver(loopMidiReceiver));
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
        trayMenu.getTrayIcon().displayMessage(APP_NAME, "All connections established successfully.", TrayIcon.MessageType.INFO);
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
