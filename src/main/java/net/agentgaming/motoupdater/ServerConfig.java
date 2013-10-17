package net.agentgaming.motoupdater;

import java.net.MalformedURLException;
import java.net.URL;

public class ServerConfig {
    private String name;
    private String ip;
    private Integer port;
    private Boolean restart;
    private String jar;
    private String[] maps;

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public Boolean shouldRestart() {
        return restart;
    }

    public URL getJar() throws MalformedURLException {
        return new URL(jar);
    }

    public String[] getMaps() {
        return maps;
    }
}
