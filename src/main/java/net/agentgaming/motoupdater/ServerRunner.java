package net.agentgaming.motoupdater;

import java.io.*;

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
        runningDir = new File("./server/" + cfg.getName() + "/");
        runningDir.mkdirs();

        start();
    }


    //TODO: Get stop to work and get running directory to work correctly
    public void start() {
        System.out.println("Running config: '" + cfg.getName() + "' on port '" + cfg.getPort() + "'");

        if (cfg.shouldRestart() || !hasStarted) {
            hasStarted = true;
            try {
                File jarName = cfg.getJar();
                if (jarName != null) {
                    for(String map : cfg.getMaps()) {
                        MotoUpdater.downloadMap(map, cfg);
                    }

                    process = Runtime.getRuntime().exec("java -server -Xmx" + cfg.getXmx() + " -Xms" + cfg.getXms() + " -jar " + jarName.getAbsolutePath() + " nogui -port=" + cfg.getPort(), null, runningDir);
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

        if (procMon.isRunning()) {
            try {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

                out.write("stop\n");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int i = 0;
                String line = null;
                while (procMon.isRunning() && (line = in.readLine()) != null) {
                    System.out.println(line);
                    Thread.sleep(1);
                    if (i == 5000) {
                        stop();
                        return;
                    }
                }
                process.waitFor();
                process.destroy();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            start();
        }

        System.out.println("Stopped config: '" + cfg.getName() + "' on port '" + cfg.getPort() + "'");
    }

    public void kill() {
        System.out.println("Killing config: '" + cfg.getName() + "' on port '" + cfg.getPort() + "'");

        if (procMon.isRunning()) {
            process.destroy();
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Killed config: '" + cfg.getName() + "' on port '" + cfg.getPort() + "'; will not restart");
    }

    private void sendCommand(String cmd) {
        System.out.println("Running cmd: '" + cmd + "' on config:: '" + cfg.getName() + "' on port '" + cfg.getPort() + "'");

        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            out.write(cmd + "\n");
            out.flush();
            out.close();
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
