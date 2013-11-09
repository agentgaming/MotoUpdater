package net.agentgaming.motoupdater;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
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

import java.io.*;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class MotoUpdater {
    private static File jarDir;
    private static File mapDir;
    private static final String key = "key1245";

    private static String username = "jxBkqvpe0seZhgfavRqB";
    private static String password = "RXaCcuuQcIUFZuVZik9K";

    private static String externalIP;

    private static HashMap<Integer, ServerRunner> servers;

    private static HttpServer server = null;

    public static void main(final String[] args) {
        Thread cmdListener = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

                try {
                    while(!in.readLine().equalsIgnoreCase("stop")) { }

                    //Stop command was run
                    in.close();
                    System.exit(0);

                    //Stop everything gracefully
                    for (ServerRunner r : MotoUpdater.getServers()) {
                        r.stop();
                    }
                    MotoUpdater.getAPIServer().stop(1);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        });
        cmdListener.start();

        Gson gson = new Gson();

        jarDir = new File("./jars/");
        jarDir.mkdirs();

        mapDir = new File("./maps/");
        mapDir.mkdirs();

        servers = new HashMap<>();

        try {
            server = HttpServer.create(new InetSocketAddress(8116), 0);
        } catch (BindException be) {
            System.out.println("Cannot start API server, stopping. Make sure port 8116 is free!");
            System.exit(0);
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
        System.out.println("Searching for servers...");

        String out = "";
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("addr", externalIP));
            out = doPost("https://agentgaming.net/api/get_servers.php", params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (out == "" || out == "," || out == "0") {
            System.out.println("Couldn't get servers or there were none.. stopping");
            System.exit(0);
        }

        //Start your engines!
        System.out.println("Found servers! Running them now...");

        for (String s : out.split(",")) {
            if (s.trim() == "" || s.trim() == "0") break;
            String jsonString = new String(Base64.decodeBase64(s));
            ServerConfig c = gson.fromJson(jsonString, ServerConfig.class);
            ServerRunner r = new ServerRunner(c);

            //Download maps here so they don't interfere or redownload
            System.out.println("Downloading maps for: " + c.getName());
            File runningDir = new File("./server/" + c.getName() + "/");
            runningDir.mkdirs();

            for(String map : c.getMaps()) {
                MotoUpdater.downloadMap(map, c);
            }

            addServer(c.getPort(), r);
            Thread t = new Thread(r);
            t.start();
        }

        Thread shutdown = new Thread(new Runnable() {
            @Override
            public void run() {
                //Forcibly kill all minecraft servers; we don't want ghost processes
                for (ServerRunner r : MotoUpdater.getServers()) {
                    r.getProcess().destroy();
                }
                MotoUpdater.getAPIServer().stop(1);
            }
        });

        Runtime.getRuntime().addShutdownHook(shutdown);
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

    public static ArrayList<ServerRunner> getServers() {
        ArrayList<ServerRunner> srvs = new ArrayList<ServerRunner>();
        for (Integer i : servers.keySet()) {
            srvs.add(servers.get(i));
        }
        return srvs;
    }

    public static void downloadMap(String map, ServerConfig sc) {
        File dir = new File("./server/" +  sc.getName() + "/" + map + "/");
        String hash;
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("id", map));
            hash = doPost("https://agentgaming.net/api/get_map_hash.php", params).trim().toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (File f : mapDir.listFiles()) {
            if (f.getName().contains(".zip")) {
                if (md5Hash(f).equalsIgnoreCase(hash)) {
                    try {
                        extractZip(f, dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;

                    }
                    return;
                }
            }
        }

        byte[] out;
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("id", map));
            out = Base64.decodeBase64(doPost("https://agentgaming.net/api/get_map.php", params));

            File mapZip = new File(mapDir.getAbsolutePath() + "/" + map + ".zip");
            if(mapZip.exists()) mapZip.delete();
            mapZip.createNewFile();

            IOUtils.write(out, new FileOutputStream(mapZip));
            extractZip(mapZip, dir);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public static HttpServer getAPIServer() {
        return server;
    }

    //Utils

    private static void extractZip(File zipFile, File newPath) throws IOException {
        int buffer = 2048;
        ZipFile zip = new ZipFile(zipFile);
        if(!newPath.exists()) newPath.mkdir();
        Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
        while (zipFileEntries.hasMoreElements()) {
            ZipEntry entry = zipFileEntries.nextElement();
            String currentEntry = entry.getName();
            File destFile = new File(newPath, currentEntry);
            File destinationParent = destFile.getParentFile();
            destinationParent.mkdirs();
            if (!entry.isDirectory()) {
                BufferedInputStream is = new BufferedInputStream(zip
                        .getInputStream(entry));
                int currentByte;
                byte data[] = new byte[buffer];
                FileOutputStream fos = new FileOutputStream(destFile);
                BufferedOutputStream dest = new BufferedOutputStream(fos, buffer);
                while ((currentByte = is.read(data, 0, buffer)) != -1) {
                    dest.write(data, 0, currentByte);
                }
                dest.flush();
                dest.close();
                is.close();
            }
        }
        zip.close();
    }

    private static String md5Hash(File f) {
        InputStream is = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            is = new FileInputStream(f);
            byte[] buffer = new byte[2048];
            int read = 0;
            while ((read = is.read(buffer)) > 0) digest.update(buffer, 0, read);
            return Hex.encodeHexString(digest.digest()).toLowerCase();
        } catch (Exception e) {
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
            }
        }
        return null;
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
