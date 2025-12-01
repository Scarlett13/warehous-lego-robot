package shared.utils;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shared.dto.old.PozyxPointDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static robot.RobotConstants.*;

public class MqttUtil {
    private MqttClient client;
    private final String tagId;
    private final Object lock = new Object();
    private boolean new_message = false;
    private volatile JSONObject last_message;
    volatile PozyxPointDTO notSmoothened = new PozyxPointDTO(0, 0);
    public final List<PozyxPointDTO> locations = new ArrayList<>();
    private float angle = 0f;

    public MqttUtil(String tagId) throws MqttException {
        this.tagId = tagId;
        client = create_client();
    }

    public Object getLock() {
        return lock;
    }

    public void shutdown() throws MqttException {
        client.disconnect();
        System.out.println("Disconnected");
        client.close();
    }

    public JSONObject getLastMessage() {
        new_message = false;
        return last_message;
    }

    public boolean isNewMessage() {
        return new_message;
    }

    public PozyxPointDTO getSmoothenedLocation(int window) {
        new_message = false;

        PozyxPointDTO smoothened = new PozyxPointDTO(0, 0);

        int count = 0;
        ListIterator<PozyxPointDTO> listIterator = locations.listIterator(locations.size());
        while (listIterator.hasPrevious() && count < window) {
            smoothened = smoothened.add(listIterator.previous());
            count++;
        }
        if (count == 0)
            count = 1;
        smoothened = smoothened.div(count);

        return smoothened;
    }

    public PozyxPointDTO getLocation() {
        return notSmoothened;
    }

    public float getAngle() {
        return angle;
    }

    private MqttClient create_client() throws MqttException {
        return create_client(MQTT_HOST, MQTT_TOPIC, MQTT_USERNAME, MQTT_PASSWORD);
    }

    private MqttClient create_client(String host, String topic, String username, String password) throws MqttException {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(host, MqttClient.generateClientId(), persistence);

            // MQTT connection option
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            // retain session
            connOpts.setCleanSession(true);

            // set callback
            client.setCallback(new Callback());

            // establish a connection
            System.out.println("Connecting to broker: " + host);
            client.connect(connOpts);
            System.out.println("Connected...");
            client.subscribe(topic);


            return client;
        } catch (MqttException me) {
            print_exception(me);
            throw me;
        }
    }

    private void print_exception(MqttException me) throws MqttException {
        System.out.println("reason " + me.getReasonCode());
        System.out.println("msg " + me.getMessage());
        System.out.println("loc " + me.getLocalizedMessage());
        System.out.println("cause " + me.getCause());
        System.out.println("excep " + me);
    }

    private class Callback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            System.out.println(cause);
            cause.printStackTrace();
            System.exit(0);
            try {
                client = create_client();
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            JSONArray jsonObject = new JSONArray(message.toString());
            synchronized (lock) {
                try {
                    for (int i = 0; i < jsonObject.length(); i++) {
                        // get the object that matches the tagId
                        JSONObject json = new JSONArray(message.toString()).getJSONObject(i);
                        if (jsonObject.getJSONObject(i).getInt("tagId") == Integer.parseInt(tagId, 16)) {
                            if (Integer.parseInt(tagId, 16) == json.getInt("tagId")) {
                                if (json.getBoolean("success")) {
                                    new_message = true;
                                    JSONObject data = json.getJSONObject("data");
//                                    System.out.println(data);
                                    JSONObject coordinates = json.getJSONObject("data").getJSONObject("coordinates");
                                    JSONObject orientation = json.getJSONObject("data").getJSONObject("orientation");
                                    locations.add(new PozyxPointDTO(coordinates.getInt("x"), coordinates.getInt("y")));
                                    notSmoothened = new PozyxPointDTO(coordinates.getInt("x"), coordinates.getInt("y"));
                                    lock.notify();
                                    angle=(float) Math.toDegrees(orientation.getFloat("yaw"));


                                } else if (!json.getBoolean("success")) {
                                    new_message = false;
                                }
                            }
                        }
                    }
                } catch (JSONException ignored) {
                    System.out.println("Malformed message received: " + message);
                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            System.out.println("Delivery complete");
        }

    }

}
