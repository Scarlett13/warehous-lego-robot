package server.agentmonitor;

import jade.core.AID;
import java.util.Set;

@Deprecated
public final class JadeAgentFilter {

    private static final Set<String> DEFAULT_JADE_AGENT_NAMES = Set.of(
            "ams",
            "df",
            "rma",
            "sniffer",
            "introspector",
            "dummy-agent"
    );

    private JadeAgentFilter() {}

    public static boolean isDefaultJadeAgentName(String localName) {
        if (localName == null) return true;

        String n = localName.trim().toLowerCase();
        if (n.isEmpty()) return true;

        // Exact known defaults
        if (DEFAULT_JADE_AGENT_NAMES.contains(n)) return true;

        if (n.startsWith("rma")) return true;
        if (n.startsWith("sniffer")) return true;
        if (n.startsWith("introspector")) return true;

        return false;
    }

    public static boolean isDefaultJadeAgentName(AID aid) {
        if (aid == null) return true;
        return isDefaultJadeAgentName(aid.getLocalName());
    }

    public static boolean shouldLogAgent(String localName) {
        return !isDefaultJadeAgentName(localName);
    }

    public static boolean shouldLogAgent(AID aid) {
        return !isDefaultJadeAgentName(aid);
    }
}

