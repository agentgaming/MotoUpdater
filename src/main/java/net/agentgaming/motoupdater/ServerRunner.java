package net.agentgaming.motoupdater;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerRunner implements Runnable {
    private Process process;
    private ServerConfig cfg;

    private File runningDir;

    private ProcMon procMon;

    public ServerRunner(ServerConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public void run() {
        File runningDir = new File("./server/");
        File jarDir = new File("./jars/");

        jarDir.mkdirs();
        runningDir.mkdirs();

        start();
    }

    public void start() {
        if(cfg.shouldRestart()) {
            try {
                //TODO: Change this.
                String jarName = cfg.getJar().getFile();
                if(jarName != null) {
                    process = Runtime.getRuntime().exec("java -jar " + MotoUpdater.getJarDir().getAbsolutePath() + jarName, null, runningDir);
                    procMon = new ProcMon(process, this);
                } else {
                    System.out.println("Jar does not exist on disk!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
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

    private void sendCommand(String cmd) {
        try {
            process.getOutputStream().write((cmd + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Process getProcess() {
        return process;
    }

    public File getRunningDir() {
        return runningDir;
    }
}
