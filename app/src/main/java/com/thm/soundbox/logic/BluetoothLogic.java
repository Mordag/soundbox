package com.thm.soundbox.logic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.thm.soundbox.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public final class BluetoothLogic {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final int MASTER_THREAD = 0;
    private final Handler mHandler;
    private final BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, ConnectedThread> mThreads;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private String mMasterAddress;
    private boolean mKeepAcceptAlive = true, mKeepConnectAlive = true, mClosing;

    public BluetoothLogic(Handler handler) {
        mHandler = handler;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        mThreads = new HashMap<>();
    }

    public void startConnection(int type) {
        switch (type) {
            case Util.MASTER:
                if (mAcceptThread == null) {
                    mAcceptThread = new AcceptThread();
                    mAcceptThread.run();
                }
                break;
            case Util.SLAVE:
                ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>(mBluetoothAdapter.getBondedDevices());
                if (!pairedDevices.isEmpty() && pairedDevices.get(MASTER_THREAD) != null && mThreads.isEmpty()) {
                    mConnectThread = new ConnectThread(pairedDevices.get(MASTER_THREAD));
                    mMasterAddress = pairedDevices.get(MASTER_THREAD).getAddress();
                    mConnectThread.run();
                }
                break;
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final String mmDeviceAddress;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmDeviceAddress = socket.getRemoteDevice().getAddress();

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmOutStream = tmpOut;
            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    if (mHandler != null) {
                        Object[] data = new Object[] {
                            buffer, mmDeviceAddress
                        };
                        mHandler.obtainMessage(0, bytes, -1, data).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.getStackTrace();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.getStackTrace();
            }
        }

        public String getDeviceAddress() {
            return mmDeviceAddress;
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Bluetooth Service", MY_UUID);
            } catch (IOException e) {
                e.getStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned
            while (mKeepAcceptAlive) {
                socket = null;

                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    e.getStackTrace();
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    ConnectedThread thread = new ConnectedThread(socket);
                    mThreads.put(thread.getDeviceAddress(), thread);
                    thread.run();
                }
            }
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.getStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.getStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            while (mKeepConnectAlive) {
                try {
                    mmSocket.connect();
                    mKeepConnectAlive = false;
                } catch (IOException connectException) {
                    mKeepConnectAlive = true;
                }
            }

            // Do work to manage the connection (in a separate thread)
            ConnectedThread thread = new ConnectedThread(mmSocket);
            mThreads.put(thread.getDeviceAddress(), thread);
            thread.run();
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.getStackTrace();
            }
        }
    }

    public boolean isMasterAvailable() {
        return !mThreads.isEmpty() && mMasterAddress != null;
    }

    public void sendDataToMaster(String data) {
        byte[] aBytes = data.getBytes(StandardCharsets.UTF_8);

        if (!mClosing) {
            mThreads.get(mMasterAddress).write(aBytes);
        }
    }

    public void sendDataToSlave(String deviceAddress, String data) {
        if(mThreads.containsKey(deviceAddress)) {
            byte[] aBytes = data.getBytes(StandardCharsets.UTF_8);

            if (!mClosing) {
                mThreads.get(deviceAddress).write(aBytes);
            }
        }
    }

    public void close() {
        mKeepAcceptAlive = false;
        mClosing = true;
        mMasterAddress = null;

        for (ConnectedThread thread : mThreads.values()) {
            thread.cancel();
        }
        mThreads.clear();

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
        }
    }
}
