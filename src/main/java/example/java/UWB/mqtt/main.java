package example.java.UWB.mqtt;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.concurrent.TimeUnit;

public class main {

    public static void main(String[] args) throws MqttException, InterruptedException {

        TagIdMqtt tag = new TagIdMqtt("682E");
        System.out.println("sanity check 2");

        while (true)
        {
            TimeUnit.SECONDS.sleep(3);
            System.out.println("data = " + tag.getData());
            System.out.println("sanity check");
            // the angle should be set on the default direction
//            System.out.println("tag angle = " + tag.getAngle());

    }
    }

}