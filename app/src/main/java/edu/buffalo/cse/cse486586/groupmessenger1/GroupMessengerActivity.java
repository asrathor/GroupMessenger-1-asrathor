package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.telephony.TelephonyManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import android.content.Context;
import android.os.AsyncTask;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    /*
     * The app listens to one server socket on port 10000.
     */
    static final int SERVER_PORT = 10000;
    /*
     * Each emulator has a specific remote port it should connect to.
     */
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * Calculates the port number this AVD listen on.
         * Taken from PA1.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        /*
         * A server socket needs to be created, in addition to a thread (AsyncTask), that listens on the server port.
         * PA1 code can be taken as a skeleton.
         */
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a server socket");
            e.printStackTrace();
        }


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
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

        /*
         * To acquire the access to the pointer to the input region where user enters the message to be sent.
         */
        final EditText edit_text = (EditText) findViewById(R.id.editText1);

        /*
         * The send button is used to deliver the message inputted from the edit_text.
         */
        Button send_button = (Button) findViewById(R.id.button4);

        /*
         * To configure the send button to show the message input by the user, setOnClickListener is used.
         * If the software keyboard is used, then setOnKeyListener should be used to listen for the enter key.
         * However, the updateavd.py was run for using hardware keyboard in the emulators.
         */
        send_button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * PA1 explains the code that can be used to retrieve the message and display it.
                 */
                String message = edit_text.getText().toString() + "\n";
                /*
                 * To reset the input box.
                 */
                edit_text.setText("");
                TextView tv = (TextView) findViewById(R.id.textView1);
                /*
                 * To display the message (string). However, this lead to printing out the sending message twice on sender's UI.
                 */
                //tv.append(message + "\t");
                //tv.append("\n");

                /*
                 * To send the string over network.
                 */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, myPort);
            }
        });

    }

    /*
     * ServerTask is created to handle the incoming messages.
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {

            ServerSocket server_socket = serverSockets[0];
            Socket client = null;
            String message;

            /*
             * A sequence number needs to be assigned to each incoming message, starting from 0.
             * The sequence has to start from 0, otherwise the testing script cannot locate the message, resulting in failure.
             * The key should be the sequence number, while the value should be the message received.
             * For building a URI for content provider, the given code from the OnPTestClickListener can be referenced.
             */
            int message_sequence = 0;
            final Uri mUri;
            mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
            /*
             * do-while is used to make sure that continous message can be exchanged until the read-half of the socket connection is open which is checked by isInputShutDown (https://developer.android.com/reference/java/net/Socket.html)
             * If not used, only one message can be send between the avds, similar to PA1.
             */
            try {

                do {
                    /*
                     * Socket is an endpoint for communication between two machines and underlying class implements CLIENT sockets. (https://developer.android.com/reference/java/net/Socket.html)
                     * The serverSocket waits for requests to come in over the network and underlying class implements SERVER sockets. (https://developer.android.com/reference/java/net/ServerSocket.html)
                     * Once serverSocket detects the incoming connection, it is first required to accept it and for communication, create a new instance of socket.
                     * The accept() method listens for a connection to be made to this socket and accepts it. (https://developer.android.com/reference/java/net/ServerSocket.html#accept())
                     */
                    client = server_socket.accept();

                    /*
                     * InputStream is superclass for all classes representing an input stream of bytes to be used with a socket. (https://developer.android.com/reference/java/net/Socket.html, https://developer.android.com/reference/java/io/InputStream.html)
                     * InputStreamReader is used to bridge from byte streams to characters. While the connection is live, the InputStreamReader is used to read one or more bytes from byte-stream. (https://developer.android.com/reference/java/io/InputStreamReader.html)
                     * BufferedReader is used to increase efficiency/performance with the InputStreamReader as it buffers the data into the memory for quick access. Use of this is optional (https://developer.android.com/reference/java/io/InputStreamReader.html)
                     * Additional source for understanding concepts to determine the relation between above three is http://www.javalearningacademy.com/streams-in-java-concepts-and-usage/
                     */

                    BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));

                    /*
                     * readLine() is used to read a line of text and obtain the message to pass on
                     */
                    message = input.readLine();

                    /*
                     * To insert the key-value pair.
                     * The code is given in the PA2 description.
                     * The key will be the respective number of the message, while the value will be the message itself.
                     * It is important to convert the message number (int) to string since all key-value should be Java strings.
                     */
                    ContentValues pair_to_insert = new ContentValues();

                    pair_to_insert.put("key", Integer.toString(message_sequence));
                    pair_to_insert.put("value", message);
                    message_sequence++;
                    Uri new_uri = getContentResolver().insert(mUri, pair_to_insert);
                    /*
                     * It is required that the incoming message should be passed onto the onProgressUpdate.
                     * However, if onProgressUpdate(message) is called directly, it results in Fatal Exception AsyncTask1 (observed in PA1) while executing doInBackground() caused by android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
                     * Android documentation on AsyncTask suggests that onProgressUpdate runs on the UI thread after PublishProgress.
                     * PublishProgress can be invoked in doInBackground to publish updates on UI thread and triggers the execution of onProgressUpdate, necessary for our task (https://developer.android.com/reference/android/os/AsyncTask.html#publishProgress(Progress...))
                     */
                    publishProgress(message);

                } while (client.isInputShutdown()!=true);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        /*
         * To build a URI for content provider. Referred from OnPTestClickListener.
         */
        private Uri buildUri(String content, String s) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(s);
            uriBuilder.scheme(content);
            return uriBuilder.build();
        }

        protected void onProgressUpdate(String...strings){

        /*
         * The following code, obtained from PA1, displays the content received in doInBackground().
         */
            String string_received = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(string_received + "\t");
            tv.append("\n");
            return;
        }
    }

    /*
     * ClientTask is an AsyncTask that sends the message (in form of string) over the network.
     * It is created whenever the send button is pressed.
     */
    private class ClientTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... strings) {

            /*
             * Each individual port will be connected to respective emulators.
             * The message received needs to be outputted across all emulators.
             */
            String[] ports = {"11108","11112","11116","11120","11124"};
            for (int i = 0; i<ports.length; i++){
                try {
                    String remote_port = ports[i];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remote_port));
                    String message = strings[0];

                    /*
                    * Inverse to the InputStreamReader which reads the message, OutputStreamWriter can be used to write the message
                    * OutputStream is superclass representing output stream of bytes (https://developer.android.com/reference/java/io/OutputStream.html)
                    * OutputStreamWriter bridge from character streams to bytes (https://developer.android.com/reference/java/io/OutputStreamWriter.html)
                    */
                    BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    /*
                    * writes the message to be send (https://developer.android.com/reference/java/io/BufferedWriter.html)
                    */
                    output.write(message);

                    /*
                    * Flush is used to flush the stream.
                    */
                    output.flush();

                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
