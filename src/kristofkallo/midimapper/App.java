package kristofkallo.midimapper;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import java.awt.*;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

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

    private final Map<String, MidiDevice> devices = new HashMap<>();

    private final TrayMenu trayMenu;
//    private MidiDevice m400In; // M-400 -> Midi Mapper
//    private MidiDevice loopMidiOut; // Midi Mapper -> loopMidi
//    private Receiver loopMidiOutReceiver;
//    private final JPanel panel;

    public App() throws FileNotFoundException, AWTException {
//        panel = new JPanel();
//        panel.setVisible(false);
        trayMenu = new TrayMenu(this);
        // Console -> this program -> loopMidi -> DAW
        devices.put("m400In", null);
        devices.put("loopMidiInCh1_16", null);
        devices.put("loopMidiInCh17_32", null);
        devices.put("loopMidiInCh33_48", null);
        devices.put("loopMidiInAux", null);
        devices.put("loopMidiInMain", null);
        // DAW -> loopMidi -> this program -> Console
        devices.put("m400Out", null);
        devices.put("loopMidiOutCh1_16", null);
        devices.put("loopMidiOutCh17_32", null);
        devices.put("loopMidiOutCh33_48", null);
        devices.put("loopMidiOutAux", null);
        devices.put("loopMidiOutMain", null);
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

            if (info.getName().contains("RSS M-400") && device.getMaxTransmitters() != 0) {
                devices.replace("m400In", device);
                try {
                    devices.get("m400In").open();
                } catch (MidiUnavailableException e) {
                    trayMenu.getTrayIcon().displayMessage(APP_NAME, "Could not open M-400 MIDI OUT port (M-400 -> PC).", TrayIcon.MessageType.ERROR);
                }
            }
            if (info.getName().contains("RSS M-400") && device.getMaxReceivers() != 0) {
                devices.replace("m400Out", device);
                try {
                    devices.get("m400Out").open();
                } catch (MidiUnavailableException e) {
                    trayMenu.getTrayIcon().displayMessage(APP_NAME, "Could not open M-400 MIDI IN port (M-400 <- PC).", TrayIcon.MessageType.ERROR);
                }
            }
            if (devices.containsKey(info.getName())) {
                if (device.getMaxReceivers() != 0 && info.getName().contains("In") ||
                    device.getMaxTransmitters() != 0 && info.getName().contains("Out")) {

                    devices.replace(info.getName(), device);
                    try {
                        devices.get(info.getName()).open();
                    } catch (MidiUnavailableException e) {
                        trayMenu.getTrayIcon().displayMessage(APP_NAME, "Could not open " + info.getName() + " port.", TrayIcon.MessageType.ERROR);
                    }
                }
            }
            /*if (info.getName().equals() && device.getMaxReceivers() != 0) {
                loopMidiOut = device;

                try {
                    loopMidiOut.open();
                } catch (MidiUnavailableException e) {
                    trayMenu.getTrayIcon().displayMessage(APP_NAME, "Could not open loopMidi IN port.", TrayIcon.MessageType.ERROR);
                }
                try {
                    loopMidiOutReceiver = loopMidiOut.getReceiver();
                } catch (MidiUnavailableException e) {
                    // This shouldn't happen
                    e.printStackTrace();
                }

            }*/
            //System.out.println(info.getName() + " transmitters:" + device.getMaxTransmitters() + " receivers:" + device.getMaxReceivers());
        }
        for (String deviceKey : devices.keySet()) {
            if (devices.get(deviceKey) == null) {
                trayMenu.getTrayIcon().displayMessage(APP_NAME, deviceKey + " device not found.", TrayIcon.MessageType.ERROR);
                return;
            }
            if (!devices.get(deviceKey).isOpen()) {
                trayMenu.getTrayIcon().displayMessage(APP_NAME, "Could not open " + deviceKey + " port. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
                return;
            }
        }
        Receiver[] receivers = new Receiver[0];
        try {
            receivers = new Receiver[]{
                    devices.get("loopMidiInCh1_16").getReceiver(),
                    devices.get("loopMidiInCh17_32").getReceiver(),
                    devices.get("loopMidiInCh33_48").getReceiver(),
                    devices.get("loopMidiInAux").getReceiver(),
                    devices.get("loopMidiInMain").getReceiver(),
            };
        } catch (MidiUnavailableException e) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "loopMidi receivers could not be retrieved. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
            e.printStackTrace();
        }
        // Receiver null checks
        for (Receiver receiver: receivers) {
            if (receiver == null) {
                trayMenu.getTrayIcon().displayMessage(APP_NAME, "One or more loopMidi receiver is null. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
                return;
            }
        }
        try {
            devices.get("m400In").getTransmitter().setReceiver(new M400Receiver(receivers));
        } catch (MidiUnavailableException e) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "m400 transmitter could not be retrieved. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
            e.printStackTrace();
        }
        /*if (m400In == null) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "M-400 device not found.", TrayIcon.MessageType.ERROR);
            return;
        }
        if (loopMidiOut == null) {
            trayMenu.getTrayIcon().displayMessage(APP_NAME, "loopMidi device not found.", TrayIcon.MessageType.ERROR);
            return;
        }
        if (m400In.isOpen() && loopMidiOut.isOpen() && loopMidiOutReceiver != null) {
            try {
                m400In.getTransmitter().setReceiver(new M400Receiver(loopMidiOutReceiver));
            } catch (MidiUnavailableException e) {
                // This shouldn't happen
                e.printStackTrace();
            }
        }*/
    }
    private void closeDevices() {
        for (String deviceKey : devices.keySet()) {
            MidiDevice device = devices.get(deviceKey);
            if (device != null) {
                device.close();
            }
            devices.replace(deviceKey, null);
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
