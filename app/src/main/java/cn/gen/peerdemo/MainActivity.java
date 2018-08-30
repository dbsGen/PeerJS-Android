package cn.gen.peerdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import cn.gen.peer.Connection;
import cn.gen.peer.PackData;
import cn.gen.peer.PeerController;


public class MainActivity extends Activity implements Connection.OnConnectionEvent, Connection.OnMessage {

    EditText serverEdit;
    Button serverButton;
    EditText peerEdit;
    Button peerButton;
    EditText dataEdit;
    Button sendButton;
    TextView output;

    PeerController peerController;
    Connection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverEdit = findViewById(R.id.server_url);
        serverButton = findViewById(R.id.server_button);
        peerEdit = findViewById(R.id.peer_id);
        peerButton = findViewById(R.id.peer_button);
        dataEdit = findViewById(R.id.data_content);
        sendButton = findViewById(R.id.send_button);
        output = findViewById(R.id.output);

        serverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String serverUrl = serverEdit.getText().toString().trim();
                if (serverUrl.length() > 0) {
                    final PeerController pc = new PeerController(MainActivity.this, serverUrl);
                    pc.setOnStatusChange(new PeerController.OnStatusChange() {
                        @Override
                        public void onPeerStatus(int status) {
                            switch (status) {
                                case PeerController.STATUS_CONNECTED: {
                                    peerController = pc;
                                    log("Server connected");

                                    break;
                                }
                                case PeerController.STATUS_CONNECTING: {
                                    log("Connecting to " + serverUrl + " ...");
                                    break;
                                }
                                case PeerController.STATUS_DISCONNECTED: {
                                    log("Server disconnected");
                                    break;
                                }
                            }
                        }
                    });
                }else {
                    log("Error: Server is empty!");
                }
            }
        });

        peerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String peerId = peerEdit.getText().toString().trim();
                if (peerId.length() == 0) {
                    log("Error: Peer ID is empty!");
                }else if (peerController == null) {
                    log("Error: must connect to server first!");
                }else {
                    Connection conn = peerController.connect(peerId);
                    conn.registerConnectionEvent(MainActivity.this);
                    conn.registerOnMessage(MainActivity.this);
                    log("Connecting to " + peerId + " ...");
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = dataEdit.getText().toString().trim();
                if (text.length() == 0) {
                    log("Error: Data text is empty!");
                }else if (connection == null) {
                    log("Error: not connect to any peer!");
                }else {
                    try {
                        PackData pd = new PackData();
                        pd.put("msg", text);
                        connection.send(pd);
                        log("Sent to " + connection.getPeer() + " (" + text + ")");
                    } catch (PackData.PackException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    void log(String text) {
        String o = output.getText().toString();
        output.setText(text + "\n" + o);
    }

    @Override
    public void onOpen(Connection conn) {
        connection = conn;
        log("Connected!");
    }

    @Override
    public void onClose(Connection conn) {
        log( conn.getPeer() + "disconnect !");
        if (connection == conn) {
            connection = null;
        }
    }

    @Override
    public void onMessage(Connection connection, PackData data) {
        log("Receive MSG: " + data.get("msg").toString());
    }
}
