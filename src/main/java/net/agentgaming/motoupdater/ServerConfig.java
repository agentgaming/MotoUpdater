package net.agentgaming.motoupdater;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class ServerConfig {
    private String name;
    private String ip;
    private Integer port;
    private String xms;
    private String xmx;
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

    public String getXms() {
        return xms;
    }

    public String getXmx() {
        return xmx;
    }

    public Boolean shouldRestart() {
        return restart;
    }

    public File getJar() {
        File f = null;

        try {
            String hash = DigestUtils.md5Hex(new URL(jar).openStream());
            f = new File(MotoUpdater.getJarDir(), hash + ".jar");

            if (!f.exists()) {
                ReadableByteChannel rbc = null;
                rbc = Channels.newChannel(new URL(jar).openStream());
                FileOutputStream fos = new FileOutputStream(f);
                fos.getChannel().transferFrom(rbc, 0, 1 << 24);
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
    }

    public String[] getMaps() {
        return maps;
    }
}
