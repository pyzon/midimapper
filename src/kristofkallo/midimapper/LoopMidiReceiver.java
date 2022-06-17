package kristofkallo.midimapper;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class LoopMidiReceiver implements Receiver {
    private final Receiver receiver;
    private final MidiMap midiMap;

    private byte channelNrpn;
    private byte paramNrpn;
    private byte valueMSB;

    public LoopMidiReceiver(Receiver receiver, MidiMap midiMap) {
        this.receiver = receiver;
        this.midiMap = midiMap;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        // Sanity check
        if (receiver == null) {
            throw new NullPointerException("receiver is null, this should not happen");
        }
        if (midiMap == null) {
            throw new NullPointerException("midiMap is null, this should not happen");
        }

        byte[] msg = message.getMessage();
        try {
            if (msg[0] == (byte) ShortMessage.CONTROL_CHANGE) {
                switch (msg[1]) {
                    case (byte) 99:
                        channelNrpn = msg[2];
                        break;
                    case (byte) 98:
                        paramNrpn = msg[2];
                        break;
                    case (byte) 6:
                        valueMSB = msg[2];
                        break;
                    case (byte) 38:

                }
                if (msg[1] == (byte) 99) {
                    channelNrpn = msg[2];
                } else if (msg[1])
            }
            System.out.println(msg[1]);
            System.out.println(msg[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("MIDI message too short");
        }
    }

    @Override
    public void close() {

    }
}
