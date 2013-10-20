package net.agentgaming.motoupdater;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;

public class APIServer implements HttpHandler {
    //0 = invalid cmd, 1 = invalid key, 2 = invalid args, 3 = success
    public void handle(HttpExchange t) throws IOException {
        //get args
        HashMap<String,String> args = new HashMap<String, String>();
        for (String pair : t.getRequestURI().getQuery().split("&")) {
            int idx = pair.indexOf("=");
            args.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }

        //run cmd
        if(args.containsKey("cmd") && args.containsKey("key")) {
            if(!MotoUpdater.getKey().equals(args.get("key"))) {
                out200(t, "1");
                return;
            } else {
                String cmd = args.get("cmd");
                Integer port = Integer.parseInt(args.get("port"));
                if(cmd.equals("stop")) {
                    MotoUpdater.getServer(port).stop();
                    out200(t, "3");
                    return;
                } else if(cmd.equals("start")) {
                    MotoUpdater.getServer(port).start();
                    out200(t, "3");
                    return;
                } else if(cmd.equals("restart")) {
                    MotoUpdater.getServer(port).stop();
                    MotoUpdater.getServer(port).start();
                    out200(t, "3");
                    return;
                } else if(cmd.equals("kill")) {
                    MotoUpdater.getServer(port).kill();
                    out200(t, "3");
                return;
                } else {
                    out200(t, "2");
                    return;
                }
            }
        } else {
            out200(t, "0");
            return;
        }
    }

    public void out200(HttpExchange t, String s) throws IOException {
        t.sendResponseHeaders(200, s.length());
        t.getResponseBody().write(s.getBytes());
        t.getResponseBody().close();
    }
}
