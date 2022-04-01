package br.com.tsinova.networkdiscoveryts.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.github.openunirest.http.Unirest;
import io.github.openunirest.http.options.Options;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Util {

    /*
        API 1 = https://macvendors.com/api (pago)
        API 2 = http://macvendors.co/api/ (free)
     */
    private static final int RESPONSE_API_MAC_VENDOR_TEXT = 0;
    private static final int RESPONSE_API_MAC_VENDOR_JSON = 1;
    private static final String API_MAC_VENDORS_PRIMARY = "http://macvendors.co/api/";
    private static final String API_MAC_VENDORS_SECUNDARY = "https://api.macvendors.com/";
    private static final String DIR_CACHE = "cache";
    //private static final String CACHE_MAC_VENDORS = DIR_CACHE + "/mac_vendors.json";
    private static final String CACHE_TYPE_DEVICES = DIR_CACHE + "/devices.json";
    //private static final String CACHE_OS_DETAILS = DIR_CACHE + "/os_details.json";
    //private static final String CACHE_PORTS_DETAILS = DIR_CACHE + "/ports_details.json";
    private static final SimpleDateFormat DATE_FORMAT_US = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.000");

    public static void setConfigUnirest() {
        try {

            Options.enableCookieManagement(false);

            SSLContext sslcontext = SSLContexts.custom().
                    loadTrustMaterial(null, new TrustSelfSignedStrategy()).
                    build();

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .disableCookieManagement()
                    .build();

            Unirest.setTimeouts(2500, 1500);
            Unirest.setDefaultHeader("Accept", "application/json");
            Unirest.setHttpClient(httpclient);
        } catch (Exception ex) {
        }

    }

    private static String getVendorByMacAdrress(String api, String mac,
            int RESPONSE_API_MAC_VENDOR) throws Exception {

        URL oracle = new URL(api + mac);
        URLConnection yc = oracle.openConnection();

        String text = "";

        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                yc.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                text += inputLine.trim();
            }
        }

        if (RESPONSE_API_MAC_VENDOR == RESPONSE_API_MAC_VENDOR_TEXT) {
            return text;
        }

        if (RESPONSE_API_MAC_VENDOR == RESPONSE_API_MAC_VENDOR_JSON) {
            JSONObject json = new JSONObject(text);
            return json.getJSONObject("result").getString("company");
        }

        return "";

    }

    public synchronized static JSONObject getTypeDevice(String mac) throws Exception {

        File dir = Paths.get(DIR_CACHE).toFile();

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = Paths.get(CACHE_TYPE_DEVICES).toFile();

        if (!file.exists()) {
            Files.write(Paths.get(CACHE_TYPE_DEVICES), "[]".getBytes(), StandardOpenOption.CREATE);
        }

        JSONArray json = new JSONArray(new JSONTokener(new FileInputStream(file)));

        for (int i = 0; i < json.length(); i++) {
            JSONObject obj = json.getJSONObject(i);
            if (obj.getString("mac").equalsIgnoreCase(mac)) {
                return obj;
            }
        }

        return null;

    }

    public synchronized static void saveTypeDevice(String mac, String category, String systems, String description) throws Exception {

        File dir = Paths.get(DIR_CACHE).toFile();

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = Paths.get(CACHE_TYPE_DEVICES).toFile();

        if (!file.exists()) {
            Files.write(Paths.get(CACHE_TYPE_DEVICES), "[]".getBytes(), StandardOpenOption.CREATE);
        }

        JSONArray json = new JSONArray(new JSONTokener(new FileInputStream(file)));

        JSONObject obj = new JSONObject();
        obj.put("mac", mac);
        obj.put("category", category == null ? JSONObject.NULL : category);
        obj.put("systems", systems == null ? JSONObject.NULL : systems);
        obj.put("description", description == null ? JSONObject.NULL : description);
        obj.put("type", "");
        json.put(obj);

        Files.write(Paths.get(CACHE_TYPE_DEVICES), json.toString().getBytes(), StandardOpenOption.WRITE);

    }

    public synchronized static void saveTypeDevice(String mac, String type[]) throws Exception {

        File dir = Paths.get(DIR_CACHE).toFile();

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = Paths.get(CACHE_TYPE_DEVICES).toFile();

        if (!file.exists()) {
            Files.write(Paths.get(CACHE_TYPE_DEVICES), "[]".getBytes(), StandardOpenOption.CREATE);
        }

        JSONArray json = new JSONArray(new JSONTokener(new FileInputStream(file)));

        JSONObject obj = new JSONObject();
        obj.put("mac", mac);
        obj.put("category", (type == null || type[1] == null) ? JSONObject.NULL : type[1]);
        obj.put("systems", (type == null || type[0] == null) ? JSONObject.NULL : type[0]);
        obj.put("type", "");
        obj.put("description", JSONObject.NULL);
        json.put(obj);

        Files.write(Paths.get(CACHE_TYPE_DEVICES), json.toString().getBytes(), StandardOpenOption.WRITE);

    }

    public synchronized static String getVendorByMacAdrress(String mac) throws Exception {

        File dir = Paths.get(DIR_CACHE).toFile();

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = Paths.get(CACHE_TYPE_DEVICES).toFile();

        if (!file.exists()) {
            Files.write(Paths.get(CACHE_TYPE_DEVICES), "[]".getBytes(), StandardOpenOption.CREATE);
        }

        JSONArray json = new JSONArray(new JSONTokener(new FileInputStream(Paths.get(CACHE_TYPE_DEVICES).toFile())));

        for (int i = 0; i < json.length(); i++) {
            JSONObject obj = json.getJSONObject(i);
            if (obj.getString("mac").equalsIgnoreCase(mac) && obj.has("vendor")) {
                return (obj.get("vendor") == JSONObject.NULL ? null : obj.getString("vendor"));
            }
        }

        // Vendor não existe no cache, busca na API
        try {
            String vendor = getVendorByMacAdrress(API_MAC_VENDORS_PRIMARY,
                    mac,
                    RESPONSE_API_MAC_VENDOR_JSON);
            insertMacVendorCache(json, mac, vendor);
            return vendor;
        } catch (Exception ex) {
        }

        try {
            String vendor = getVendorByMacAdrress(API_MAC_VENDORS_SECUNDARY,
                    mac,
                    RESPONSE_API_MAC_VENDOR_TEXT);
            insertMacVendorCache(json, mac, vendor);
            return vendor;
        } catch (Exception ex) {
            throw ex;
        }

    }

    private static void insertMacVendorCache(JSONArray json, String mac, String vendor) throws Exception {

        File dir = Paths.get(DIR_CACHE).toFile();

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = Paths.get(CACHE_TYPE_DEVICES).toFile();

        if (!file.exists()) {
            Files.write(Paths.get(CACHE_TYPE_DEVICES), "[]".getBytes(), StandardOpenOption.CREATE);
        }

        boolean flag = false;

        for (int i = 0; i < json.length(); i++) {
            JSONObject obj = json.getJSONObject(i);
            if (obj.getString("mac").equalsIgnoreCase(mac)) {
                obj.put("vendor", (vendor == null ? JSONObject.NULL : vendor));
                flag = true;
                break;
            }
        }

        if (!flag) {
            JSONObject obj = new JSONObject();
            obj.put("mac", mac);
            obj.put("vendor", (vendor == null ? JSONObject.NULL : vendor));
            json.put(obj);
        }

        Files.write(Paths.get(CACHE_TYPE_DEVICES), json.toString().getBytes(), StandardOpenOption.WRITE);

    }

    public static String getDatetimeForElasticsearch(Date date) {
        String dateFormated = DATE_FORMAT_US.format(date) + "T" + TIME_FORMAT.format(date) + "Z";
        return dateFormated;
    }

    public static String getDatetimeNowForElasticsearch() {
        return Instant.now().toString();
    }

    public synchronized static String getOsDeviceFromNMap(String passwordSudo,
            String ip, String mac) throws Exception {

        File dir = Paths.get(DIR_CACHE).toFile();

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = Paths.get(CACHE_TYPE_DEVICES).toFile();

        if (!file.exists()) {
            Files.write(Paths.get(CACHE_TYPE_DEVICES), "[]".getBytes(), StandardOpenOption.CREATE);
        }

        JSONArray json = new JSONArray(new JSONTokener(new FileInputStream(Paths.get(CACHE_TYPE_DEVICES).toFile())));

        for (int i = 0; i < json.length(); i++) {
            JSONObject obj = json.getJSONObject(i);
            if (obj.getString("mac").equalsIgnoreCase(mac) && obj.has("os")) {
                return (obj.get("os") == JSONObject.NULL ? null : obj.getString("os"));
            }
        }

        String os = null;

        try {
            os = Util.getOsDeviceFromNMap(passwordSudo, ip);
        } catch (Exception ex) {
        }

        boolean flag = false;

        for (int i = 0; i < json.length(); i++) {
            JSONObject obj = json.getJSONObject(i);
            if (obj.getString("mac").equalsIgnoreCase(mac)) {
                obj.put("os", (os == null) ? JSONObject.NULL : os);
                flag = true;
                break;
            }
        }

        if (!flag) {

            JSONObject obj = new JSONObject();
            obj.put("mac", mac);
            obj.put("os", (os == null) ? JSONObject.NULL : os);
            json.put(obj);

        }

        Files.write(Paths.get(CACHE_TYPE_DEVICES), json.toString().getBytes(), StandardOpenOption.WRITE);

        return os;

    }

    private synchronized static String getOsDeviceFromNMap(String passwordSudo, String ip) throws Exception {

        String[] cmd = {"/bin/bash", "-c", "echo " + passwordSudo + "| sudo -S nmap -O --osscan-limit " + ip};

        Process p = Runtime.getRuntime().exec(cmd);

        if (!p.waitFor(10, TimeUnit.SECONDS)) {
            p.destroy();
            return null;
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String lineOut;

        while ((lineOut = input.readLine()) != null) {
            if (lineOut.startsWith("OS details:")) {
                return lineOut.substring(lineOut.indexOf(":") + 1).trim();
            }
        }

        input.close();
        p.destroy();

        return null;

    }

    public static int getSecondsByPeriod(Date dStart, Date dEnd) {
        DateTime start = new DateTime(dStart);
        DateTime end = new DateTime(dEnd);
        int seconds = Seconds.secondsBetween(start, end).getSeconds();
        return seconds;
    }

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);

        return bd.doubleValue();

    }

    public static Object executeScript(String script, Object value) throws Exception {

        Binding binding = new Binding();
        GroovyShell shell = new GroovyShell(binding);

        Object valueScript;

        if (value instanceof String) {

            valueScript = "\"" + value.toString() + "\"";

        } else if (value instanceof Integer || value instanceof Double || value instanceof Float || value instanceof Long) {

            valueScript = value;

        } else {
            throw new Exception("only strings, integers, doubles, floats, and longs are accepted");

        }

        script = script.replace("{{value}}", valueScript.toString());

        Object response = shell.evaluate(script);

        return response;

    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = new HashSet<>();
        return t -> seen.add(keyExtractor.apply(t));
    }
    
    public static String prettyPrinter(JSONObject value) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    mapper.readValue(value.toString(), Object.class));
        } catch (Exception ex) {
            return value.toString();
        }
    }

    public static String prettyPrinter(Object value) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    value);
        } catch (Exception ex) {
            return value.toString();
        }
    }
    
    public static JSONObject getJson(JSONObject jsonValuesDefault, JSONObject jsonValues) throws JSONException {

        JSONObject json = new JSONObject();

        Iterator it = jsonValuesDefault.keys();
        while (it.hasNext()) {
            String key = it.next().toString();
            json.put(key, jsonValuesDefault.get(key));
        }

        it = jsonValues.keys();
        while (it.hasNext()) {
            String key = it.next().toString();
            json.put(key, jsonValues.get(key));
        }

        return json;
    }

}
