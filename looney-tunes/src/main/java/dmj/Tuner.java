import java.util.Scanner;
import javax.sound.sampled.*;
import be.tarsos.dsp.*;
import be.tarsos.dsp.io.jvm.*;
import be.tarsos.dsp.pitch.*;
import be.tarsos.dsp.util.fft.FFT;
import java.util.Arrays;

public class Tuner {
    public static void main(String[] args) {
        final int SAMPLE_RATE = 44100;
        final int BUFFER_SIZE = 1024;

        Scanner scanner = new Scanner(System.in);

        System.out.println("Press Enter to start the tuner...");
        scanner.nextLine(); // Wait for user input

        try {
            // Configure microphone
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            // Create audio stream
            JVMAudioInputStream audioStream = new JVMAudioInputStream(microphone);

            // Create audio dispatcher
            AudioDispatcher dispatcher = new AudioDispatcher(audioStream, BUFFER_SIZE, 0);

            // Add pitch detection handler
            dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, SAMPLE_RATE, BUFFER_SIZE, new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                    float pitchInHz = pitchDetectionResult.getPitch();
                    if (pitchInHz != -1) {
                        String note = convertToNote(pitchInHz);
                        System.out.println("Detected pitch: " + pitchInHz + " Hz (" + note + ")");
                    } else {
                        System.out.println("No pitch detected");
                    }
                }
            }));

            // Start audio processing
            dispatcher.run();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } finally {
            scanner.close(); // Close the scanner
        }
    }

    // Convert pitch in Hz to musical note
    private static String convertToNote(float pitchInHz) {
        double semitoneRatio = Math.pow(2, 1.0 / 12);
        double c0 = 16.35; // Frequency of C0 in Hz
        int noteNumber = (int) Math.round(12 * Math.log(pitchInHz / c0) / Math.log(semitoneRatio));
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int octave = noteNumber / 12 - 1;
        int noteIndex = noteNumber % 12;
        return noteNames[noteIndex] + octave;
    }
}
