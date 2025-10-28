package example.java.UWB.mqtt;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.concurrent.TimeUnit;

public class main {

    public static void main(String[] args) throws MqttException, InterruptedException {

        UWB.mqtt.TagIdMqtt tag = new UWB.mqtt.TagIdMqtt("685C");

        while (true)
        {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("tag location = " + tag.getLocation());
            // the angle should be set on the default direction
            System.out.println("tag angle = " + tag.getAngle());

    }
    }

}