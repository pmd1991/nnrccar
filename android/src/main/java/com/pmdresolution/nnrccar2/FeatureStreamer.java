package com.pmdresolution.nnrccar2;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by jlim on 12/27/13.
 */
public class FeatureStreamer {
    private Socket sock;
    private DataOutputStream dos;
    private int mport;

    FeatureStreamer() {
    }

    void connect(String addr, int port) {
        mport = port;
        ConnectTask task = new ConnectTask();
        task.execute(addr);
    }

    void sendByteArray(byte[] send) throws IOException {
        if (dos != null) {
            dos.writeInt(send.length);
            dos.write(send, 0, send.length);
            dos.flush();
        }
    }

    void sendFeatures(int width, int height, byte[] send, float[] accelerometerFeatures) {
        try {
            if (dos != null) {
                dos.writeInt(width);
                dos.writeInt(height);
                dos.writeInt(accelerometerFeatures.length);
                dos.write(send, 0, send.length);
                for (int i = 0; i < accelerometerFeatures.length; i++) {
                    dos.writeFloat(accelerometerFeatures[i]);
                }
                dos.flush();
            }
        } catch (IOException e) {
        }
    }

    void close() throws IOException {
        if (dos != null) {
            dos.close();
        }
        if (sock != null) {
            sock.close();
        }
    }

    class ConnectTask extends AsyncTask<String, Integer, Long> {

        private Exception exception;

        protected Long doInBackground(String... addr) {
            try {
                sock = new Socket(addr[0], mport);
                dos = new DataOutputStream(sock.getOutputStream());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0L;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Void result) {
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }
};

