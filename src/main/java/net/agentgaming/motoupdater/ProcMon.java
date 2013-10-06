package net.agentgaming.motoupdater;

public class ProcMon implements Runnable {

    private final Process _proc;
    private volatile boolean _complete = false;

    public boolean isRunning() { return !_complete; }

    public ProcMon(Process proc) {
        this._proc = proc;
    }

    public void run() {
        try {
            _proc.waitFor();
        } catch (InterruptedException e) {
        }
        _complete = true;
        MotoUpdater.start();
    }

    public static ProcMon create(Process proc) {
        ProcMon procMon = new ProcMon(proc);
        Thread t = new Thread(procMon);
        t.start();
        return procMon;
    }
}