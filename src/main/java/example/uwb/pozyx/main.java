package example.uwb.pozyx;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.concurrent.TimeUnit;

public class main {

    public static void main(String[] args) throws MqttException, InterruptedException {

        TagIdMqtt tag = new TagIdMqtt("6823");

        while (true)
        {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("tag location = " + tag.getLocation());
            // the angle should be set on the default direction
            System.out.println("tag angle = " + tag.getAngle());

        }
    }

}