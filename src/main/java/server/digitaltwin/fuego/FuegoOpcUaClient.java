package server.digitaltwin.fuego;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import shared.dto.Point2D;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuegoOpcUaClient {

    private static final String ENDPOINT_URL = "opc.tcp://192.168.0.179:4840";
    private static final NodeId LOCATION_NODE_ID = new NodeId(2, "Robot1/Location");

    private OpcUaClient client;
    private volatile Point2D currentPosition = new Point2D(0, 0);
    private volatile double currentAngle = 0.0;
    private boolean connected = false;

    public void connect() {
        try {
            System.out.println("🔥 Connecting to Fuego OPC UA: " + ENDPOINT_URL);

            client = OpcUaClient.create(
                    ENDPOINT_URL,
                    endpoints -> endpoints.stream().findFirst(),
                    configBuilder -> configBuilder
                            .setIdentityProvider(new AnonymousProvider())
                            .build());

            client.connect().get();
            connected = true;
            System.out.println("🔥 Connected to Fuego OPC UA Server!");

            // Start polling loop
            startPolling();

        } catch (Exception e) {
            System.err.println("🔥 Failed to connect to Fuego OPC UA: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    private void startPolling() {
        new Thread(() -> {
            while (connected) {
                try {
                    readLocation();
                    Thread.sleep(500); // 2Hz polling
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("🔥 Error reading location: " + e.getMessage());
                }
            }
        }).start();
    }

    private void readLocation() {
        if (!connected || client == null)
            return;

        try {
            CompletableFuture<DataValue> future = client.readValue(0.0, TimestampsToReturn.Both, LOCATION_NODE_ID);
            DataValue dataValue = future.get();
            Variant variant = dataValue.getValue();

            if (variant != null && !variant.isNull()) {
                String locStr = variant.getValue().toString();
                // Expected format: "70150.000;147590.000;0.000" (X;Y;Angle)
                parseLocation(locStr);
            }
        } catch (Exception e) {
            // System.err.println("Failed to read value");
        }
    }

    private void parseLocation(String locStr) {
        if (locStr == null) {
            System.err.println("🔥 Error parsing location string: null");
            return;
        }
        try {
            // "70150.000;147590.000;0.000"
            String[] parts = locStr.split(";");
            if (parts.length >= 2) {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);

                this.currentPosition = new Point2D((int) x, (int) y);

                if (parts.length >= 3) {
                    this.currentAngle = Double.parseDouble(parts[2]);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("🔥 Error parsing location string: " + locStr);
        }
    }

    public Point2D getPosition() {
        return currentPosition;
    }

    public double getAngle() {
        return currentAngle;
    }

    public boolean isConnected() {
        return connected;
    }
}
