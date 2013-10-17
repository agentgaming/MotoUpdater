package net.agentgaming.motoupdater;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class MotoUpdater {
    private static File jarDir;
    private static final String key = "thisisakey";

    private static int nextPort = 8116;

    private static HashMap<Integer, ServerRunner> servers = new HashMap<Integer, ServerRunner>();

    public static void main(final String[] args) {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(cfg.getPort()), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.createContext("/", new APIServer());
        server.setExecutor(null);
        server.start();

        //TODO: actually get the configs
        ServerConfig[] servers = new ServerConfig[] {};

        for(ServerConfig c : servers) {
            ServerRunner r = new ServerRunner(c);
            addServer(c.getPort(), r);
            Thread t = new Thread(r);
            t.start();
        }
    }

    public static File getJarDir() {
        return jarDir;
    }

    public static String getKey() {
        return key;
    }

    public static void addServer(Integer i, ServerRunner r) {
        servers.put(i, r);
    }

    public static ServerRunner getServer(Integer port) {
        return servers.get(port);
    }
}
