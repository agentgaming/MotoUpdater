package net.agentgaming.motoupdater;

import java.io.File;
import java.io.IOException;

public class ServerRunner implements Runnable {
    private Process process;
    private ServerConfig cfg;

    private File runningDir;
    private ProcMon procMon;

    private boolean hasStarted = false;

    public ServerRunner(ServerConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public void run() {
        File runningDir = new File("./server/" + cfg.getName() + "/");
        runningDir.mkdirs();

        start();
    }

    public void start() {
        System.out.println("Running config: '" + cfg.getName() + "' on port '" + cfg.getPort() + "'");

        if(cfg.shouldRestart() || !hasStarted) {
            hasStarted = true;
            try {
                File jarName = cfg.getJar();
                if(jarName != null) {
                    process = Runtime.getRuntime().exec("java -jar " + jarName.getAbsolutePath(), null, runningDir);
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
        System.out.println("Stopping config: '" + cfg.getName() + "' on port '" + cfg.getPort() + "'");

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

        System.out.println("Stopped config: '" + cfg.getName() + "' on port '" + cfg.getPort() + "'");
    }

    private void sendCommand(String cmd) {
        System.out.println("Running cmd: '" + cmd + "' on config:: '" + cfg.getName() + "' on port '" + cfg.getPort() + "'");

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
