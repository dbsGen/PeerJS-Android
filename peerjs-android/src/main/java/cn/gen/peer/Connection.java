package cn.gen.peer;

import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by mac on 2018/3/4.
 */

public class Connection implements PeerConnection.Observer, DataChannel.Observer, BandwidthLimiter.OnBandwidthFree {

    public final static int SERIALIZATION_BINARY = 0;
    public final static int SERIALIZATION_BINARY_UTF8 = 1;
    public final static int SERIALIZATION_JSON = 2;
    public final static int SERIALIZATION_UNKOWN = 3;

    String peer;
    String clientId;
    PeerController controller;
    PeerConnection peerConnection;
    DataChannel dataChannel;
    ArrayList<DataChannel.Buffer> needToSend = new ArrayList<>();

    boolean sendChunk = false;

    static final int chunkedMTU = 16300;

    static final String PREFIX = "dc_";

    static int dataCount;

    @Override
    public void onBandwidthFree() {
        if (needToSend.size() > 0) {
            DataChannel.Buffer[] buffers = new DataChannel.Buffer[needToSend.size()];
            needToSend.toArray(buffers);
            needToSend.clear();
            for (DataChannel.Buffer data : buffers) {
                sendRaw(data);
            }
        }
    }

    public interface OnConnectionEvent {
        void onOpen(Connection conn);
        void onClose(Connection conn);
    }

    public interface OnMessage {
        void onMessage(Connection connection, PackData data);
    }

    private static PeerConnectionFactory connectionFactory = null;
    ArrayList<OnConnectionEvent> onConnectionEvents = new ArrayList<>();
    ArrayList<OnMessage> onMessages = new ArrayList<>();

    int serialization = SERIALIZATION_UNKOWN;

    public String getPeer() {
        return peer;
    }

    public String getClientId() {
        return clientId;
    }

    public Connection(String peer, PeerController controller, JSONObject payload) {
        this.peer = peer;

        try {
            this.clientId = payload.getString("connectionId");
            String ser = payload.getString( "serialization");
            if ("binary".equals(ser)) {
                serialization = SERIALIZATION_BINARY;
            }else if ("binary-utf8".equals(ser)) {
                serialization = SERIALIZATION_BINARY_UTF8;
            }else if ("json".equals(ser)) {
                serialization = SERIALIZATION_JSON;
            }
        }catch (Exception e) {

        }
        this.controller = controller;
    }

    public void registerConnectionEvent(OnConnectionEvent onConnectionEvent) {
        onConnectionEvents.add(onConnectionEvent);
    }
    public void removeConnectionEvent(OnConnectionEvent onConnectionEvent) {
        onConnectionEvents.remove(onConnectionEvent);
    }

    private String randomToken() {
        long l = (long)(Math.random() * 1000000);
        long t = new Date().getTime() % 1000000;
        return Long.toHexString(l + t);
    }

    public Connection(String peer, PeerController controller) {
        this.controller = controller;
        this.peer = peer;

        this.clientId = PREFIX + randomToken();

        serialization = SERIALIZATION_BINARY;
    }

    public static void setUp(Context context) {

        PeerConnectionFactory.initialize(PeerConnectionFactory
                .InitializationOptions
                .builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.networkIgnoreMask = 0;
        connectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .createPeerConnectionFactory();

    }

    public void connect() {
        if (peerConnection == null)
            makeConnection();
        DataChannel.Init init = new DataChannel.Init();
        init.ordered = true;
        init.negotiated = false;
        init.maxRetransmits = -1;
        init.maxRetransmitTimeMs = -1;
        init.id = -1;
        init.protocol = "";
        dataChannel = peerConnection.createDataChannel(this.clientId, init);

        dataChannel.registerObserver(this);
        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(final SessionDescription offsetSdp) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {

                    }

                    @Override
                    public void onSetSuccess() {
                        try {
                            JSONObject json = new JSONObject();
                            json.put("type", "OFFER");
                            json.put("dst", peer);
                            JSONObject payload = new JSONObject();
                            payload.put("sdp", makeJson(offsetSdp));
                            payload.put("type", "data");
                            payload.put("label", clientId);
                            payload.put("connectionId", clientId);
                            payload.put("reliable", true);
                            payload.put("serialization", "binary");
                            payload.put("browser", "Chrome");
                            json.put("payload", payload);
                            controller.send(json);
                        }catch (Exception e) {

                        }
                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                }, offsetSdp);
            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, new MediaConstraints());
    }

    public void makeConnection() {
        String uri = "stun:stun.xten.com";
        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(Collections.singletonList(
                PeerConnection.IceServer.builder(uri).createIceServer()
        ));

        peerConnection = connectionFactory.createPeerConnection(config, this);
    }

    public static JSONObject makeJson(SessionDescription sdp) {
        if (sdp != null) {
            JSONObject res = new JSONObject();
            try {
                switch (sdp.type) {
                    case OFFER:
                        res.put("type", "offer");
                        break;
                    case ANSWER:
                        res.put("type", "answer");
                        break;
                    case PRANSWER:
                        res.put("type", "pranswer");
                        break;
                }
                res.put("sdp", sdp.description);
            }catch (JSONException e) {

            }
            return res;
        }
        return null;
    }

    public static JSONObject makeJson(IceCandidate candidate) {
        if (candidate != null) {
            try {
                JSONObject json = new JSONObject();
                json.put("candidate", candidate.sdp);
                json.put("sdpMid", candidate.sdpMid);
                json.put("sdpMLineIndex", candidate.sdpMLineIndex);
                return json;
            }catch (JSONException e) {

            }
        }
        return null;
    }

    public void onMessage(JSONObject message) {
        String type = null;
        try {
            type = message.getString("type");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if ("CANDIDATE".equals(type)) {
            try {
                JSONObject payload = message.getJSONObject("payload");
                onCandidate(payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else if ("ANSWER".equals(type)) {
            try {
                JSONObject payload = message.getJSONObject("payload");
                onAnswer(payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onAnswer(JSONObject answer) {
        if (peerConnection == null) {
            Log.e("Connection", "Peer not init");
        }
        String sdp = null;
        try {
            sendChunk = "chrome".equals(answer.getString("browser").toLowerCase());
            JSONObject sdpObject = answer.getJSONObject("sdp");
            sdp = sdpObject.getString("sdp");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (sdp == null) {
            Log.e("Connection", "have not got sdp");
            return;
        }

        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, new SessionDescription(SessionDescription.Type.ANSWER, sdp));
    }

    public void onCandidate(JSONObject payload) {
        try {
            JSONObject candidate = payload.getJSONObject("candidate");
            peerConnection.addIceCandidate(new IceCandidate(
                    candidate.getString("sdpMid"),
                    candidate.getInt("sdpMLineIndex"),
                    candidate.getString("candidate")
            ));
        }catch (JSONException e) {
        }
    }

    public void onOffer(JSONObject offer) {
        if (peerConnection == null) {
            makeConnection();
        }

        String sdp = null;
        try {
            sendChunk = true; //"chrome".equals(offer.getString("browser").toLowerCase());
            JSONObject sdpObject = offer.getJSONObject("sdp");
            sdp = sdpObject.getString("sdp");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (sdp == null) {
            Log.e("Connection", "have not got sdp");
            return;
        }

        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {
                peerConnection.createAnswer(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(final SessionDescription answerSDP) {
                        peerConnection.setLocalDescription(new SdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {

                            }

                            @Override
                            public void onSetSuccess() {
                                try {
                                    JSONObject json = new JSONObject();
                                    json.put("type", "ANSWER");
                                    json.put("dst", peer);
                                    JSONObject payload = new JSONObject();
                                    payload.put("sdp", makeJson(answerSDP));
                                    payload.put("type", "data");
                                    payload.put("connectionId", clientId);
                                    payload.put("browser", "Chrome");
                                    json.put("payload", payload);
                                    controller.send(json);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onCreateFailure(String s) {

                            }

                            @Override
                            public void onSetFailure(String s) {

                            }
                        }, answerSDP);
                    }

                    @Override
                    public void onSetSuccess() {

                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                }, new MediaConstraints());
            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, new SessionDescription(SessionDescription.Type.OFFER, sdp));
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "CANDIDATE");
            json.put("dst", peer);
            JSONObject payload = new JSONObject();
            payload.put("candidate", makeJson(iceCandidate));
            payload.put("type", "data");
            payload.put("connectionId", clientId);
            json.put("payload", payload);
            controller.send(json);
        } catch (JSONException e) {

        }
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        if (this.dataChannel != null) {
            this.dataChannel.unregisterObserver();
        }
        this.dataChannel = dataChannel;
        this.dataChannel.registerObserver(this);
    }

    @Override
    public void onRenegotiationNeeded() {
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
    }

    private ByteBuffer[] makeChunks(ByteBuffer buffer) throws PackData.PackException, IOException {
        int size = buffer.remaining();
        int total = (int)Math.ceil( size / (double)chunkedMTU);

        ByteBuffer[] chunks = new ByteBuffer[total];
        int start = 0, index = 0;
        while (start < size) {
            int end = Math.min(size, start + chunkedMTU);

            byte[] bf = new byte[end - start];
            buffer.get(bf);

            PackData chunk = new PackData();
            chunk.put("__peerData", dataCount);
            chunk.put("n", index);
            chunk.put("data", bf);
            chunk.put("total", total);

            chunks[index] = chunk.pack();

            ++index;
            start = end;
        }
        ++dataCount;
        return chunks;
    }

    public void send(PackData data) {

        try {
            ByteBuffer buffer = data.pack();
            if (sendChunk && buffer.remaining() > chunkedMTU) {
                ByteBuffer[] chunks = makeChunks(buffer);
                for (ByteBuffer chunk :
                        chunks) {
                    sendRaw(new DataChannel.Buffer(chunk, true));
                }
            }else {
                sendRaw(new DataChannel.Buffer(buffer, true));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendRaw(DataChannel.Buffer buffer) {
        if (canSend()) {
            int len = buffer.data.remaining();
            if (dataChannel.send(buffer)) {
                controller.getBandwidthLimiter().sendData(len);
            }
        }else {
            needToSend.add(buffer);
        }
    }

    private boolean canSend() {
        return dataChannel.state() == DataChannel.State.OPEN && controller.getBandwidthLimiter().canSend();
    }

    @Override
    public void onBufferedAmountChange(long l) {
    }

    @Override
    public void onStateChange() {

        if (dataChannel.state() == DataChannel.State.OPEN) {
            if (needToSend.size() > 0) {
                DataChannel.Buffer[] buffers = new DataChannel.Buffer[needToSend.size()];
                needToSend.toArray(buffers);
                needToSend.clear();
                for (DataChannel.Buffer data : buffers) {
                    sendRaw(data);
                }
            }

            controller.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    controller.getBandwidthLimiter().addFreeListener(Connection.this);
                    for (OnConnectionEvent event :
                            onConnectionEvents) {
                        event.onOpen(Connection.this);
                    }
                }
            });
        }
        else if (dataChannel.state() == DataChannel.State.CLOSED) {
            controller.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    controller.getBandwidthLimiter().removeFreeListener(Connection.this);
                    for (OnConnectionEvent event :
                            onConnectionEvents) {
                        event.onClose(Connection.this);
                    }
                }
            });
        }
    }

    public void registerOnMessage(OnMessage onMessage) {
        onMessages.add(onMessage);
    }

    public void removeOnMessage(OnMessage onMessage) {
        onMessages.remove(onMessage);
    }

    private class DataSet {
        int count;
        Object[] datas;

        public DataSet(int total) {
            this.count = 0;
            this.datas = new Object[total];
        }

        public void set(int idx, byte[] data) {
            if (idx < datas.length && datas[idx] == null) {
                datas[idx] = data;
                count ++;
            }
        }

        public byte[] buf() {
            if (count == datas.length) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                for (Object data : datas) {
                    try {
                        os.write((byte[]) data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return os.toByteArray();
            }
            return null;
        }
    }

    private HashMap<Object, DataSet> chunkedDatas = new HashMap();

    @Override
    public void onMessage(final DataChannel.Buffer buffer) {
        PackData data = PackData.unpack(buffer.data);
        PackData dataId = data.get("__peerData");
        if (dataId == null) {
            controller.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (onMessages.size() > 0) {
                        OnMessage[] arr = new OnMessage[onMessages.size()];
                        onMessages.toArray(arr);
                        for (OnMessage message : arr) {
                            message.onMessage(Connection.this, data);
                        }
                    }
                }
            });
        }else {
            Object key = dataId.get();
            PackData td = data.get("total");
            if (td != null) {
                int total = td.intValue();
                DataSet set = null;
                if (chunkedDatas.containsKey(key)) {
                    set = chunkedDatas.get(key);
                }else {
                    set = new DataSet(total);
                    chunkedDatas.put(key, set);
                }
                PackData nd = data.get("n");
                set.set(nd.intValue(), data.get("data").bufferValue());
                if (set.count == set.datas.length) {
                    byte[] bytes = set.buf();
                    ByteBuffer buf = ByteBuffer.allocate(bytes.length);
                    buf.put(bytes);
                    buf.rewind();
                    PackData allData = PackData.unpack(buf);
                    chunkedDatas.remove(key);
                    controller.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            if (onMessages.size() > 0) {
                                OnMessage[] cs = new OnMessage[onMessages.size()];
                                onMessages.toArray(cs);
                                for (OnMessage message : cs) {
                                    message.onMessage(Connection.this, allData);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    public void close() {
        peerConnection.close();
    }
}
