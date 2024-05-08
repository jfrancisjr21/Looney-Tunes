package dmj;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.sound.sampled.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.transform.FastFourierTransformer;

public class GuitarTunerApp extends Application {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Label statusLabel;
    private LineChart<Number, Number> chart;
    private XYChart.Series<Number, Number> series;
    private TargetDataLine line;
    private boolean listening = false;

    @Override
    public void start(Stage primaryStage) {
        statusLabel = new Label("Click 'Start' to begin detecting guitar tune");

        // Create the chart
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Frequency (Hz)");
        yAxis.setLabel("Magnitude");
        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Fourier Transform");
        series = new XYChart.Series<>();
        chart.getData().add(series);

        Button toggleButton = new Button("Start Listening");
        toggleButton.setOnAction(e -> toggleListening());

        VBox root = new VBox(statusLabel, chart, toggleButton);

        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("Guitar Tuner");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void toggleListening() {
        if (listening) {
            stopListening();
        } else {
            startListening();
        }
    }

    private void startListening() {
        statusLabel.setText("Listening for microphone input...");
        listening = true;

        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            scheduler.scheduleAtFixedRate(this::analyzeSound, 0, 500, TimeUnit.MILLISECONDS);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    private void stopListening() {
        statusLabel.setText("Click 'Start' to begin detecting guitar tune");
        listening = false;
        scheduler.shutdown();
        line.stop();
        line.close();
    }

    private void analyzeSound() {
        byte[] buffer = new byte[1024];
        int bytesRead = line.read(buffer, 0, buffer.length);

        double[] samples = new double[bytesRead / 2];
        for (int i = 0, s = 0; i < bytesRead; i += 2, s++) {
            samples[s] = (buffer[i + 1] << 8 | (buffer[i] & 0xFF)) / 32768.0;
        }

        // Perform Fourier Transform
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] transformed = transformer.transform(samples, TransformType.FORWARD);

        // Update the chart with the Fourier transform
        updateChart(transformed);
    }

    private void updateChart(Complex[] transformed) {
        series.getData().clear();
        double maxFreq = 0;
        double maxMag = 0;

        // Find the peak frequency and magnitude
        for (int i = 0; i < transformed.length / 2; i++) {
            double freq = i * 44100.0 / transformed.length;
            double mag = transformed[i].abs();
            if (mag > maxMag) {
                maxMag = mag;
                maxFreq = freq;
            }
        }

        // Update the chart with the Fourier transform
        for (int i = 0; i < transformed.length / 2; i++) {
            double freq = i * 44100.0 / transformed.length;
            double mag = transformed[i].abs();
            series.getData().add(new XYChart.Data<>(freq, mag));
        }

        // Determine the closest note
        double closestNoteFreq = findClosestNoteFrequency(maxFreq);
        String closestNote = getNoteName(closestNoteFreq);

        // Update status label with detected note
        statusLabel.setText("Detected note: " + closestNote + " (" + maxFreq + " Hz)");
    }

    private double findClosestNoteFrequency(double frequency) {
        // Define frequencies for each note
        double[] noteFrequencies = {
                82.41, 87.31, 92.50, 98.00, 103.83, 110.00, 116.54, 123.47, 130.81, 138.59, 146.83, 155.56,
                164.81, 174.61, 185.00, 196.00, 207.65, 220.00, 233.08, 246.94, 261.63, 277.18, 293.66, 311.13,
                329.63, 349.23, 369.99, 392.00, 415.30, 440.00, 466.16, 493.88, 523.25, 554.37, 587.33, 622.25,
                659.25, 698.46, 739.99, 783.99, 830.61, 880.00, 932.33, 987.77, 1046.50, 1108.73, 1174.66, 1244.51,
                1318.51
        };

        double minDiff = Double.MAX_VALUE;
        double closestFreq = 0;

        // Find the closest note frequency
        for (double noteFreq : noteFrequencies) {
            double diff = Math.abs(frequency - noteFreq);
            if (diff < minDiff) {
                minDiff = diff;
                closestFreq = noteFreq;
            }
        }

        return closestFreq;
    }

    private String getNoteName(double frequency) {
        // Define note names
        String[] noteNames = {
                "E2", "F2", "F#2/Gb2", "G2", "G#2/Ab2", "A2", "A#2/Bb2", "B2", "C3", "C#3/Db3", "D3", "D#3/Eb3",
                "E3", "F3", "F#3/Gb3", "G3", "G#3/Ab3", "A3", "A#3/Bb3", "B3", "C4", "C#4/Db4", "D4", "D#4/Eb4",
                "E4", "F4", "F#4/Gb4", "G4", "G#4/Ab4", "A4", "A#4/Bb4", "B4", "C5", "C#5/Db5", "D5", "D#5/Eb5",
                "E5", "F5", "F#5/Gb5", "G5", "G#5/Ab5", "A5", "A#5/Bb5", "B5", "C6", "C#6/Db6", "D6", "D#6/Eb6",
                "E6"
        };

        // Define the corresponding frequencies for each note
        double[] noteFrequencies = {
                82.41, 87.31, 92.50, 98.00, 103.83, 110.00, 116.54, 123.47, 130.81, 138.59, 146.83, 155.56,
                164.81, 174.61, 185.00, 196.00, 207.65, 220.00, 233.08, 246.94, 261.63, 277.18, 293.66, 311.13,
                329.63, 349.23, 369.99, 392.00, 415.30, 440.00, 466.16, 493.88, 523.25, 554.37, 587.33, 622.25,
                659.25, 698.46, 739.99, 783.99, 830.61, 880.00, 932.33, 987.77, 1046.50, 1108.73, 1174.66, 1244.51,
                1318.51
        };

        // Find the index of the closest note frequency
        int index = 0;
        double minDifference = Math.abs(frequency - noteFrequencies[0]);
        for (int i = 1; i < noteFrequencies.length; i++) {
            double difference = Math.abs(frequency - noteFrequencies[i]);
            if (difference < minDifference) {
                minDifference = difference;
                index = i;
            }
        }

        return noteNames[index];
    }

    public static void main(String[] args) {
        launch(args);
    }
}
