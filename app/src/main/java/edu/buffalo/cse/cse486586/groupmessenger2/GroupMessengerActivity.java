package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    private static final String TAG = GroupMessengerActivity.class.getSimpleName();

    private static final Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    private static final HashMap<String,Integer> REMOTE_PORTS = new HashMap<String,Integer>(){{
        put("11108",1);put("11112",2);put("11116",3);put("11120",4);put("11124",5);
    }};
    private static final int SERVER_PORT=10000;

    public static ConcurrentLinkedQueue<Message> holdBackQueue = new ConcurrentLinkedQueue<Message>();
    public static ConcurrentLinkedQueue<Message> deliveryQueue = new ConcurrentLinkedQueue<Message>();
    private Integer messageId = 0;
    private Integer proposed = 0;
    private Integer delivered = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        try {
            //Create a server socket as well as a thread (AsyncTask) that listens on the server port.
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        final EditText editText = (EditText)findViewById(R.id.editText1);
        tv.append(editText.getText().toString());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {

                        String msg = editText.getText().toString() + "\n";
                        editText.setText("");

                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket = null;
            String strPortNumber = getPortNumber();
            try {
                String msg;

                while(true) {
                    socket = serverSocket.accept();
                    //socket.setSoTimeout(2000);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

                    if ((msg = reader.readLine()) != null) {
                        if(msg.matches(".*%%%.*%%%.*")){
                            Message newMessage = new Message();

                            String[] msgParts = msg.split("%%%");
                            proposed = proposed + 1;
                            String proposedSeqNum = String.valueOf(proposed) +"." +REMOTE_PORTS.get(strPortNumber);

                            newMessage.setMsg(msgParts[0]);
                            newMessage.setSequenceNumber(Double.parseDouble(proposedSeqNum));
                            newMessage.setMessageId(Integer.valueOf(msgParts[1]));
                            newMessage.setProcessId(REMOTE_PORTS.get(msgParts[2]));

                            holdBackQueue.add(newMessage);
                            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                            writer.println(proposedSeqNum);
                        }else {

                            Message toDeliver = new Message();
                            String msgSplit[] = msg.split("%%");
                            for (Message findMsg : holdBackQueue) {
                                if (findMsg.getMsg().equals(msgSplit[3]) && findMsg.getMessageId()== Integer.valueOf(msgSplit[1])) {
                                    findMsg.setSequenceNumber(Double.valueOf(msgSplit[0]));
                                    findMsg.setDeliverable(true);
                                    toDeliver = findMsg;
                                }
                            }
                            holdBackQueue.remove(toDeliver);  //to reorder message
                            holdBackQueue.add(toDeliver);
                            int i = 0;
                            while(!holdBackQueue.isEmpty() && holdBackQueue.peek().isDeliverable()){
                                Message del = holdBackQueue.poll();
                                deliveryQueue.add(del);
                                publishProgress(del.getMsg() + "\n");       //deliver the message
                            }
                        }
                    }
                    reader.close();
                }
            }catch (IOException ioe){
                Log.e(TAG, "ServerTask socket IOException");
            }finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error in Socket close");
                }
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {

            String strSequence = String.valueOf(delivered);
            String strMessage = strings[0].trim();
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(strMessage);

            ContentValues keyValueToInsert = new ContentValues();

            keyValueToInsert.put("key", strSequence);
            keyValueToInsert.put("value", strMessage);

            /*getContentResolver().insert(
                    providerUri,    // assume we already created a Uri object with our provider URI
                    keyValueToInsert
            );*/
            return;
        }


    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            Double finalSeqNumber = 0.0;
            String msgReceived = null;
            String msgToSend = msgs[0];
            String destinationPort = null;
            String thisPort = getPortNumber();
            try {

                messageId = messageId + 1;
                for( String strPort:REMOTE_PORTS.keySet()) {
                    destinationPort = strPort;
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(strPort));

                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    StringBuilder sendMsg = new StringBuilder();
                    sendMsg.append(msgToSend.replace("\n","") + "%%%" +String.valueOf(messageId)+"%%%"+thisPort);
                    writer.println(sendMsg.toString());
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    if ((msgReceived = reader.readLine()) != null) {
                        finalSeqNumber = Math.max(finalSeqNumber, Double.parseDouble(msgReceived));
                        Log.e(TAG, String.valueOf(finalSeqNumber));
                    }
                    writer.flush();
                    reader.close();
                    socket.close();
                }

                for( String strPort:REMOTE_PORTS.keySet()) {
                    destinationPort = strPort;
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(strPort));

                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    writer.println(finalSeqNumber+"%%"+ String.valueOf(messageId)+"%%"+strPort+"%%"+msgToSend);

                    socket.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (SocketTimeoutException e) {
                //handleException(destinationPort);
                Log.e(TAG, "ClientTask SocketTimeoutException");
            } catch (IllegalBlockingModeException e) {
                Log.e(TAG, "ClientTask IllegalBlockingModeException");
            } catch (IOException e) {
                //handleException(destinationPort);
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
        public void handleException(String port){
            String destinationPort = null;
            try {
                Log.e(TAG, "handling exception");
                REMOTE_PORTS.remove(port);
                for (String strPort : REMOTE_PORTS.keySet()) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(strPort));
                    destinationPort = strPort;

                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    writer.println(String.valueOf(REMOTE_PORTS.get(port)));

                    socket.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (SocketTimeoutException e) {
                handleException(destinationPort);
                Log.e(TAG, "ClientTask SocketTimeoutException");
            } catch (IllegalBlockingModeException e) {
                Log.e(TAG, "ClientTask IllegalBlockingModeException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
        }
    }


    private static Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private String getPortNumber() {
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        return String.valueOf((Integer.parseInt(portStr) * 2));
    }


}
