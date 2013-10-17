package net.agentgaming.motoupdater;

public class ProcMon implements Runnable {

    private final Process _proc;
    private final ServerRunner r;
    private volatile boolean _complete = false;

    public boolean isRunning() { return !_complete; }

    public ProcMon(Process proc, ServerRunner r) {
        this._proc = proc;
        this.r = r;
    }

    public void run() {
        try {
            _proc.waitFor();
        } catch (InterruptedException e) {
        }
        _complete = true;
        r.start();
    }

    public static ProcMon create(Process proc, ServerRunner r) {
        ProcMon procMon = new ProcMon(proc, r);
        Thread t = new Thread(procMon);
        t.start();
        return procMon;
    }
}