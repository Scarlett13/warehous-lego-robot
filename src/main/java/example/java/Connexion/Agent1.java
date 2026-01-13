package example.java.Connexion;

import example.java.UWB.mqtt.TagIdMqtt;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import org.eclipse.paho.client.mqttv3.MqttException;


import java.awt.geom.Point2D;

public class Agent1 extends Agent {
    int[] path = new int[]{};
    int path_iterator = 0;

    static int ultra_front =0;
    static int ultra_left =0;
    static int ultra_right =0;


    float[] vals;

    long previousTime = System.currentTimeMillis();

    int setPoint = 8;  //65

    int lastError1 = 0;
    int lastError2 = 0;

    int cumError1 = 0;
    int cumError2 = 0;

    float Kp = 7F;
    float Ki = 0.00001F;
    float Kd = 10.2F;

    int value = 0;
    static TagIdMqtt tag;

    static {
        try {
            tag = new TagIdMqtt("685C");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    CyclicBehaviour obstacle_check = new CyclicBehaviour() {
        @Override
        public void action() {
            try {
                value = Device2.check_Emergency();
                //   System.out.println(" LEFT "+ultra_left+" "+" RIGHT "+ultra_right);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour go_forward = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.forward();
                System.out.println("Forward");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour go_backward = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.backward();
                System.out.println("Backward");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour turn_right = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.turnRight();
                System.out.println("Right");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour turn_left = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.turnLeft();
                System.out.println("Left");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour stop = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.stop();
//                System.out.println("Stop");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    protected void setup() {
        System.out.println("local name"+getAID().getLocalName());
        System.out.println("GloBal name"+getAID().getName());
/*
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println("OneShot");
            }
        });

        addBehaviour(new TickerBehaviour(this, 10000) {
            @Override
            protected void onTick() {
                System.out.println("Tick");
            }
        });*/
        /*
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                System.out.println("Cyclic");
            }
        });*/
        // addBehaviour(obstacle_check);
        addBehaviour(move);
    }

    example.java.UWB.helpers.Point2D loc;
    Behaviour move = new Behaviour() {
        @Override
        public void action() {
            try {
                System.out.println("Entering move.action()");

                loc = tag.getSmoothenedLocation(10);
                if (loc == null || (loc.x == 0 && loc.y == 0)) {
                    System.out.println("Esperant fix Pozyx vàlid per al tag 685C...");
                    block(500);
                    return;
                }
                System.out.println("Tag returned: x=" + loc.x + ", y=" + loc.y);
                int x = loc.x;
                int y = loc.y;

                if (x != 0 && y != 0) {
                    int target_x = path[path_iterator];
                    int target_y = path[path_iterator + 1];

                    System.out.println("x: " + x);
                    System.out.println("y: " + y);

                    float yaw = tag.getAngle();
                    // yaw = (float) (yaw-301.5);
                    System.out.println("yaw mission="+yaw);

                    double diff_y = target_y - y;
                    double diff_x = target_x - x;

                    System.out.println("diff_y="+diff_y);
                    System.out.println("diff_x="+diff_x);


                    double dist = Point2D.distance(x, y, target_x, target_y);

                    //double atan2 =Math.atan2(diff_y, diff_x);

                    //System.out.println("atan2="+atan2);

                    //float target_angle = (float) Math.toDegrees(atan2); //??


                    float target_angle = (float) Math.toDegrees(Math.atan2((double)(target_y - y), (double)(target_x - x)));



                    System.out.println("Target Angle"+target_angle);

                    float diff_angle = target_angle - yaw;
                    diff_angle = (float) (((diff_angle + 540.0) % 360.0) - 180.0);


                    System.out.println("- AFTER Diff Angle**"+diff_angle);


                    if (Math.abs(target_x - x) < 100 && Math.abs(target_y - y) < 100) {    // old params diff_angle > 10 && diff_angle <= 180, diff_angle < 350 && diff_angle > 180
//                        if (path_iterator >= path.length - 1) {}
//                        path_iterator += 2;
                        Device2.setSpeed(0);
                        addBehaviour(stop);
                    } else if (diff_angle > 10) {
                        Device2.setSpeed(250);
                        addBehaviour(turn_right);
                        System.out.println("RIGHT");
                    } else if (diff_angle < -10) {
                        Device2.setSpeed(250);
                        addBehaviour(turn_left);
                        System.out.println("LEFT");
                    } else {
                        Device2.setSpeed(200);
                        addBehaviour(go_forward);
                        System.out.println("FORWARD");
                    }
                }

            } catch (Exception e) {
                System.out.println(e);
            }
        }

        @Override
        public boolean done() {
            return path_iterator >= path.length;
        }
    };

    @Override
    protected void takeDown() {
    }

   /* Behaviour move = new Behaviour() {
        @Override
        public void action() {
            try {
                if (value>25){
                    value=25;
                }
                if (value < 15) {
                    addBehaviour(turn_left);
                }else {
                    Device2.setSpeed(value*10);
                    Delay.msDelay(1);
                    Device2.forward();
                    Delay.msDelay(1);
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        @Override
        public boolean done() {
            return false;
        }

        // @Override
        //public boolean done() {
           // return path_iterator >= path.length;
        //}
    };*/
}