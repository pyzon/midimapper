# Midi mapper

This application is a solution to utilize a [digital mixing console](https://en.wikipedia.org/wiki/Digital_mixing_console) as a [DAW](https://en.wikipedia.org/wiki/Digital_audio_workstation) [controller](https://en.wikipedia.org/wiki/MIDI_controller) in a live mixing scenario.

There are devices specifically designed for this purpose (like the [X-TOUCH](https://www.behringer.com/product.html?modelCode=P0B1X)) that would not need additional programming. However, we already had an old mixer (particularly a [Roland M-400](https://proav.roland.com/global/products/m-400/)) that was laying unused, so we decided to give this concept a try.

The program transforms the [System Exclusive](https://en.wikipedia.org/wiki/MIDI#System_Exclusive_messages) messages of the console into [NRPN](https://en.wikipedia.org/wiki/NRPN) messages that can be read by a DAW (particularly [Cubase](https://new.steinberg.net/cubase/)). The mappings in the program and in the DAW are set up so that changing a parameter on the console propagates through the system and causes the corresponding parameter in the DAW to change, and likely, the other way around. This way, the two parts of the system are in sync, meaning that the values of the parameters are approximately equal (there is some inaccuracy due to limited precision of the data).

The program is written in Java with the use of the [javax.sound.midi](https://docs.oracle.com/javase/7/docs/api/javax/sound/midi/package-summary.html) package. It also takes advantage of [LoopMidi](https://www.tobias-erichsen.de/software/loopmidi.html) virtual MIDI devices. These are loopback devices, they immediately send out the messages they receive. We need these as links between the program and the DAW. Below is a block diagram of the whole system.

![alt](https://github.com/pyzon/midimapper/blob/master/MIDI%20mapper%20block%20diagram.png)

## Java MIDI package

The program uses the `javax.sound.midi` package. The most important classes/interfaces in this application are `MidiDevice`, `Receiver`, `Transmitter`, `MidiMessage`, `ShortMessage` and `SysexMessage`.

A `MidiDevice` is what the OS sees when you plug in a physical device. We can get an array of the currently available MIDI devices in the system with the `MidiSystem.getMidiDeviceInfo()` function. Usually, each device has two instances, for an IN and an OUT port. An IN port is the one that sends data from the device to the OS, and it is represented as a MidiDevice that can have a `Transmitter`. On the other hand, an OUT port is the one that sends data from the OS back to the device. This one can have a `Receiver`.

On the block diagram above, the blue box with a label 'T' represents a `Transmitter` and the red box with a label 'R' represents a `Receiver`.

The messages coming from transmitters can be captured by custom receivers that implement the `Receiver` interface. In order to do this, first, we need to pass our receiver to the transmitter's `setReceiver()` function as parameter. Second, in our receiver implementation, the `send(MidiMessage message, long timeStamp)` overridden method should handle the transmitter's messages.

To send a message from the program to a receiver, we can simply call its `send` method, and pass a subclass of `MidiMessage`, particularly `ShortMessage` for Control Change, and `SysexMessage` for SysEx messages.

## 