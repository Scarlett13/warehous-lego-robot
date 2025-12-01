package server;

import java.util.HashMap;
import java.util.Map;

public class AgentRegistry<T extends NamedInterface> {
    private final Map<String, T> agents = new HashMap<>();

    public void add(T agent) {
        agents.put(agent.getName(), agent);
    }

    public T get(String name) {
        return agents.get(name);
    }

    public boolean remove(String name) {
        return agents.remove(name) != null;
    }
}
