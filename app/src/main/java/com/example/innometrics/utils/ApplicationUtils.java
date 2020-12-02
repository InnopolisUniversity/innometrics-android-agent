package com.example.innometrics.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import com.github.mikephil.charting.data.PieEntry;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Ordering;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Some methods and constants to use are defined here
 */
public class ApplicationUtils {
    private static final String TAG = "ApplicationUtils";
    public static DateFormat sFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    public static final int LOG_LEVEL_ERROR = 1;
    public static final int LOG_LEVEL_WARN = 2;
    public static final int LOG_LEVEL_INFO = 3;
    public static final int LOG_LEVEL_DEBUG = 4;
    public static final int LOG_LEVEL_VERBOSE = 5;
    public static final int sCurrentLevel = 4;
    public static boolean ERROR = sCurrentLevel >= LOG_LEVEL_ERROR;
    public static boolean WARN = sCurrentLevel >= LOG_LEVEL_WARN;
    public static boolean INFO = sCurrentLevel >= LOG_LEVEL_INFO;
    public static boolean DEBUG = sCurrentLevel >= LOG_LEVEL_DEBUG;
    public static boolean VERBOSE = sCurrentLevel >= LOG_LEVEL_VERBOSE;

    /**
     * delete all files from where SharedPreferences are stored
     */
    public static void clearPreferences(Context context) {

        try {
            if(DEBUG) Log.d(TAG, "clearPreferences");
            String packageName = "com.example.innometrics";
            File sharedPreferenceFile = new File("/data/data/"+ packageName + "/shared_prefs/");
            File[] listFiles = sharedPreferenceFile.listFiles();
            if(DEBUG) Log.d(TAG, "files to delete:");
            for (File file : listFiles) {
                String fileName = file.getName();
                if(DEBUG) Log.d(TAG, fileName);
                String prefName = fileName.substring(0, fileName.length() - 4);
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return float list from given JSONArray if its elements can be converted
     */
    public static List<Float> floatArrayListFromJSONArray(JSONArray jsonArray){
        List<Float> floats = new ArrayList<>();
        if(DEBUG) Log.d(TAG, "floatsArrayListFromJSONArray");
        if(DEBUG) Log.d(TAG, jsonArray.toString());
        try {
            Object first = jsonArray.get(0);
            //server sends nulls, which are treated by JSON library as different object
            //so we have to check for Double / Integer /String everytime and stop if it is not
            //Problem originally, this problem was only with Doubles, but then somehow I converted Doubles to Integer. It is not normal behavior
            //Update: turns out only numbers with non-zero value are converted to ints (presented to ints). It can cause problems,
            if (first instanceof String){
                if(DEBUG) Log.d(TAG, "string");
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object value = jsonArray.get(i);
                    if (value instanceof String){
                        floats.add(Float.parseFloat(jsonArray.getString(i)));
                    } else {
                        break;
                    }
                }
            } else if (first instanceof Double || first instanceof Float){
                if(DEBUG) Log.d(TAG, "float");
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object value = jsonArray.get(i);
                    if (value instanceof Double){ //!jsonArray.isNull(0) ||
                        Double valueDouble = (Double) value;
                        floats.add(valueDouble.floatValue());
                    } else {
                        break;
                    }
                }
            } else if (first instanceof Integer) {
                if(DEBUG) Log.d(TAG, "integers");
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object value = jsonArray.get(i);
                    if (value instanceof Integer){
                        Integer valueDouble = (int) value;
                        floats.add(valueDouble.floatValue());
                    } else {
                        break;
                    }
                }
            } else {
                if(DEBUG) Log.d(TAG, "nor float or string");
                return null;
            }
            return floats;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * From given urls extract domain name without "www."
     */
    public static Map<String, Integer> getDomainNamesFromURLs(JSONArray urls){
        HashMap<String, Integer> domains = new HashMap<>();
//        String patternStr = "/^(?:https?:\\/\\/)?(?:[^@\\n]+@)?(?:www\\.)?([^:\\/\\n]+)/im";
        String ipAddressPattern =
                "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
        Pattern pattern = Pattern.compile(ipAddressPattern);
        try {
            for (int i = 0; i < urls.length(); i++) {
                String url = urls.getString(i);
                String found = null;
                if (url.startsWith("http")) {
                    URL aURL = new URL(url);
                    found = aURL.getHost();
                    if (found.startsWith("www.")){
                        found = found.substring(4);
                    }
                } else {
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()){
                        found = matcher.group();
                    }
                }
                if(found != null) {
                    Integer domainCount = domains.get(found);
                    if (domainCount != null) {
                        domains.put(found, domainCount + 1);
                    } else {
                        domains.put(found, 1);
                    }
                }
            }
            return domains;
        } catch (JSONException| MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * From given map[domain name, number of times domain name is visited]
     * get n-1 top PieEntries + 1 consisting of all the rest
     */
    public static List<PieEntry> getPieChartData(Map<String, Integer> data, int n){
        if(DEBUG) Log.d(TAG, "getPieChartData");
        n = Math.min(n, data.size());
        List<PieEntry> pieEntries = new ArrayList<>();

        Function<String, Integer> getVal = Functions.forMap(data);
        Ordering<String> ordering = Ordering.natural().onResultOf(getVal);
        List<String> topKeys = ordering.greatestOf(data.keySet(), n > 0 ? (n - 1)  : 0);
        List<String> others = ordering.leastOf(data.keySet(), data.size() - n + 1);

        for (int i = 0; i < topKeys.size(); i++) {
            pieEntries.add(new PieEntry(data.get(topKeys.get(i)),topKeys.get(i)));
        }
        int othersSum = 0;
        for (int i = 0; i < others.size(); i++) {
            othersSum += data.get(others.get(i));
        }
        pieEntries.add(new PieEntry(othersSum, "others"));

        return pieEntries;
    }

    /**
     * Converts JSONObject to map.
     * JSONObject must contain no array
     */
    public static Map<String, Integer> convertToMap(JSONObject jsonObject)  throws JSONException {
        Map<String, Integer> map = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                value = convertToMap((JSONObject) value);
            }
            map.put(key, (int) value);
        }   return map;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Activity activity) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Taken from: https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
     * Convert byte array to hex string
     * @param bytes toConvert
     * @return hexValue
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sbuf = new StringBuilder();
        for(int idx=0; idx < bytes.length; idx++) {
            int intVal = bytes[idx] & 0xff;
            if (intVal < 0x10) sbuf.append("0");
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    /**
     * Taken from: https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
     * Get utf8 byte array.
     * @param str which to be converted
     * @return  array of NULL if error was found
     */
    public static byte[] getUTF8Bytes(String str) {
        try { return str.getBytes("UTF-8"); } catch (Exception ex) { return null; }
    }

    /**
     * Taken from: https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
     * Load UTF8withBOM or any ansi text file.
     * @param filename which to be converted to string
     * @return String value of File
     * @throws java.io.IOException if error occurs
     */
    public static String loadFileAsString(String filename) throws java.io.IOException {
        final int BUFLEN=1024;
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
            byte[] bytes = new byte[BUFLEN];
            boolean isUTF8=false;
            int read,count=0;
            while((read=is.read(bytes)) != -1) {
                if (count==0 && bytes[0]==(byte)0xEF && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF ) {
                    isUTF8=true;
                    baos.write(bytes, 3, read-3); // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read);
                }
                count+=read;
            }
            return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
        } finally {
            try{ is.close(); } catch(Exception ignored){}
        }
    }

    /**
     * Taken from: https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return  mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) return "";
                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) buf.append(String.format("%02X:",aMac));
                if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                return buf.toString();
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    /**
     * Taken from: https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

}
