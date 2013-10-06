package net.agentgaming.motoupdater;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class MotoUpdater {
    private static Process process;
    private static ServerCommand cmds;

    private static File jarDir;
    private static File runningDir;

    private static ProcMon procMon;

    private final static String key = "thisisakey";

    public static void main(final String[] args) {
        cmds = new ServerCommand();

        String name = "default";
        if(args.length > 0) name = args[0];

        File runningDir = new File("./server/");
        File jarDir = new File("./jars/");

        jarDir.mkdirs();
        runningDir.mkdirs();

        start();

        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(8116), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.createContext("/" + name, new Server());
        server.setExecutor(null);
        server.start();
    }

    public static void start() {
        if(cmds.shouldStart()) {
            try {
                String jarName = cmds.getJarName();
                if(jarName != null) {
                    process = Runtime.getRuntime().exec("java -jar " + jarDir.getAbsolutePath() + jarName, null, runningDir);
                    procMon = new ProcMon(process);
                } else {
                    System.out.println("Jar does not exist on disk!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void stop() {
        if(procMon.isRunning()) {
            try {
                process.getOutputStream().write("stop\n".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendCommand(String cmd) {
        try {
            process.getOutputStream().write((cmd + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Process getProcess() {
        return process;
    }

    public static File getJarDir() {
        return jarDir;
    }

    public static File getRunningDir() {
        return runningDir;
    }

    public static String getKey() {
        return key;
    }
}
