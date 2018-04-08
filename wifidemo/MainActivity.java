package com.jhard.wifidemo;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.os.AsyncTask;

import com.github.mikephil.charting.charts.LineChart;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity implements MicrophoneInputListener
{

    // Tag for logging
    private final String TAG = getClass().getSimpleName();

    // AsyncTask object that manages the connection in a separate thread
    WiFiSocketTask wifiTask = null;

    // UI elements
    TextView textStatus;
    Button buttonConnect;
    ChartMan chart;
    ChartMan chart2;
    ChartMan chart3;
    ChartMan chart4;
    private volatile boolean mDrawing;
    private volatile int mDrawingCollided;
    double mRmsSmoothed;
    double mAlpha = 0.9;
    double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
    Microphone microphone;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi);

        // Save references to UI elements
        textStatus = (TextView)findViewById(R.id.textStatus);
        buttonConnect = (Button)findViewById(R.id.connect);

        // in this example, a LineChart is initialized from xml
        chart = new ChartMan((LineChart) findViewById(R.id.chart), this);
        chart2 = new ChartMan((LineChart) findViewById(R.id.chart2), this);
        chart3 = new ChartMan((LineChart) findViewById(R.id.chart2), this);
        chart4 = new ChartMan((LineChart) findViewById(R.id.chart2), this);
        microphone = new Microphone(this);
    }

    /**
     * Helper function, print a status to both the UI and program log.
     */
    void setStatus(String s) {
        Log.v(TAG, s);
        textStatus.setText(s);
    }

    /**
     * Try to start a connection with the specified remote host.
     */
    public void connectButtonPressed(View v) {
        microphone.turnOn();
        if(wifiTask != null) {
            setStatus("Already connected!");
            return;
        }

        try {
            // Get the remote host from the UI and start the thread
            String host = "192.168.1.104";//editTextAddress.getText().toString();
            int port = 80;//Integer.parseInt(editTextPort.getText().toString());

            // Start the asyncronous task thread
            setStatus("Attempting to connect...");
            wifiTask = new WiFiSocketTask(host, port);
            wifiTask.execute();

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Invalid address/port!");
        }
    }

    /**
     * Disconnect from the connection.
     */
    public void disconnectButtonPressed(View v)
    {
        microphone.turnOff();
        if(wifiTask == null) {
            setStatus("Already disconnected!");
            return;
        }

        wifiTask.disconnect();
        setStatus("Disconnecting...");
    }

    /**
     * Invoked by the AsyncTask when the connection is successfully established.
     */
    private void connected()
    {
        setStatus("Connected.");
    }
    /**
     * Invoked by the AsyncTask when the connection ends..
     */
    private void disconnected() {
        setStatus("Disconnected.");
        //textRX.setText("");
        //textTX.setText("");
        wifiTask = null;
    }

    /**
     * Invoked by the AsyncTask when a newline-delimited message is received.
     */
    private void gotMessage(String msg) {
        //textRX.setText(msg);
        Log.v(TAG, "[RX] " + msg);
    }

    /**
     * Send the message typed in the input field using the AsyncTask.
     */
    public void sendButtonPressed(View v) {

        if(wifiTask == null) return;
    }
    public void processAudioFrame(short[] audioFrame) {
        if (!mDrawing) {
            mDrawing = true;
            // Compute the RMS value. (Note that this does not remove DC).
            double rms = 0;
            for (int i = 0; i < audioFrame.length; i++) {
                rms += audioFrame[i]*audioFrame[i];
            }
            rms = Math.sqrt(rms/audioFrame.length);

            // Compute a smoothed version for less flickering of the display.
            mRmsSmoothed = mRmsSmoothed * mAlpha + (1 - mAlpha) * rms;
            final double rmsdB = 20.0 * Math.log10(mGain * mRmsSmoothed);
            mDrawing = false;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    chart.addValue((float)rmsdB);
                }
            }, 0);
        } else {
            mDrawingCollided++;
            Log.v("debug", "Level bar update collision, i.e. update took longer " +
                    "than 20ms. Collision count" + Double.toString(mDrawingCollided));
        }
    }
    /**
     * AsyncTask that connects to a remote host over WiFi and reads/writes the connection
     * using a socket. The read loop of the AsyncTask happens in a separate thread, so the
     * main UI thread is not blocked. However, the AsyncTask has a way of sending data back
     * to the UI thread. Under the hood, it is using Threads and Handlers.
     */
    public class WiFiSocketTask extends AsyncTask<Void, String, Void> {

        // Location of the remote host
        String address;
        int port;

        // Special messages denoting connection status
        private static final String PING_MSG = "SOCKET_PING";
        private static final String CONNECTED_MSG = "SOCKET_CONNECTED";
        private static final String DISCONNECTED_MSG = "SOCKET_DISCONNECTED";

        Socket socket = null;
        BufferedReader inStream = null;
        OutputStream outStream = null;

        // Signal to disconnect from the socket
        private boolean disconnectSignal = false;

        // Socket timeout - close if no messages received (ms)
        private int timeout = 5000;

        // Constructor
        WiFiSocketTask(String address, int port) {
            this.address = address;
            this.port = port;
        }

        /**
         * Main method of AsyncTask, opens a socket and continuously reads from it
         */
        @Override
        protected Void doInBackground(Void... arg) {

            try {

                // Open the socket and connect to it
                socket = new Socket();
                socket.connect(new InetSocketAddress(address, port), timeout);

                // Get the input and output streams
                inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outStream = socket.getOutputStream();

                // Confirm that the socket opened
                if(socket.isConnected()) {

                    // Make sure the input stream becomes ready, or timeout
                    long start = System.currentTimeMillis();
                    while(!inStream.ready()) {
                        long now = System.currentTimeMillis();
                        if(now - start > timeout) {
                            Log.e(TAG, "Input stream timeout, disconnecting!");
                            disconnectSignal = true;
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "Socket did not connect!");
                    disconnectSignal = true;
                }

                // Read messages in a loop until disconnected
                while(!disconnectSignal) {

                    // Parse a message with a newline character
                    String msg = inStream.readLine();

                    // Send it to the UI thread
                    publishProgress(msg);
                }

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error in socket thread!");
            }

            // Send a disconnect message
            publishProgress(DISCONNECTED_MSG);

            // Once disconnected, try to close the streams
            try {
                if (socket != null) socket.close();
                if (inStream != null) inStream.close();
                if (outStream != null) outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * This function runs in the UI thread but receives data from the
         * doInBackground() function running in a separate thread when
         * publishProgress() is called.
         */
        @Override
        protected void onProgressUpdate(String... values) {

            String msg = values[0];
            if(msg == null) return;

            // Handle meta-messages
            if(msg.equals(CONNECTED_MSG)) {
                connected();
            } else if(msg.equals(DISCONNECTED_MSG))
                disconnected();
            else if(msg.equals(PING_MSG))
            {}

            // Invoke the gotMessage callback for all other messages
            else
            {
                gotMessage(msg);
                String[] msgs = msg.split("_");
                //chart.addValue(Float.parseFloat(msgs[1]));
                chart2.addValue(Float.parseFloat(msgs[0])); // temperature
                chart3.addValue(Float.parseFloat(msgs[1])); // Sound
                float accelox = Float.parseFloat(msgs[2]);
                float acceloy = Float.parseFloat(msgs[3]);
                float acceloz = Float.parseFloat(msgs[4]);
                float accelo = (float)Math.sqrt(accelox * accelox + acceloy * acceloy + acceloz * acceloz);
                chart4.addValue(accelo); // Accelo

            }

            super.onProgressUpdate(values);
        }

        /**
         * Write a message to the connection. Runs in UI thread.
         */
        public void sendMessage(String data) {

            try {
                outStream.write(data.getBytes());
                outStream.write('\n');
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Set a flag to disconnect from the socket.
         */
        public void disconnect() {
            disconnectSignal = true;
        }
    }
}
