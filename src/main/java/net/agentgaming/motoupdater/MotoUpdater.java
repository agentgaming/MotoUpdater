package net.agentgaming.motoupdater;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class MotoUpdater {
    private static File jarDir;
    private static final String key = "thisisakey";

    private static String username = "jxBkqvpe0seZhgfavRqB";
    private static String password = "RXaCcuuQcIUFZuVZik9K";

    private static String externalIP;

    private static HashMap<Integer, ServerRunner> servers = new HashMap<Integer, ServerRunner>();

    public static void main(final String[] args) {
        Gson gson = new Gson();

        File jarDir = new File("./jars/");
        jarDir.mkdirs();

        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(8116), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.createContext("/", new APIServer());
        server.setExecutor(null);
        server.start();

        //Get our external ip
        InputStream is = null;
        try {
            is = new URL("http://checkip.amazonaws.com/").openStream();
            externalIP = IOUtils.toString(is);
        } catch (IOException e) {
            System.out.println("Couldn't get external IP.. stopping");
            System.exit(0);
        } finally {
            IOUtils.closeQuietly(is);
        }

        //Request a list of servers for this ip
        String out = "";
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("addr", externalIP));
            out = doPost("https://agentgaming.net/api/get_plugin.php", params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(out == "" || out == "," || out == "0") {
            System.out.println("Couldn't get servers or there were none.. stopping");
            System.exit(0);
        }

        //Start your engines!
        for(String s : out.split(",")) {
            String jsonString = new String(Base64.decodeBase64(s));
            ServerConfig c = gson.fromJson(jsonString, ServerConfig.class);
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

    private static String doPost(String url, List<NameValuePair> params) throws Exception {
        List<NameValuePair> data = new ArrayList<>();
        data.add(new BasicNameValuePair("key", key));
        data.addAll(params);

        Credentials creds = new UsernamePasswordCredentials(username, password);

        return basicAuthPost(url, data, creds);
    }

    private static String basicAuthPost(String url, List<NameValuePair> params, Credentials creds) throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();

        HttpParams param = client.getParams();
        HttpConnectionParams.setConnectionTimeout(param, 120000);
        HttpConnectionParams.setSoTimeout(param, 120000);

        HttpPost post = new HttpPost(url);
        Header authHeader = new BasicScheme().authenticate(creds, post, new BasicHttpContext());
        post.addHeader(authHeader);
        post.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
        HttpResponse resp = client.execute(post);
        String respString = IOUtils.toString(resp.getEntity().getContent());
        return respString;
    }
}
