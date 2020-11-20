package rs.igram.kiribi.service.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Properties;

public class ConfigUtils {
	
	public static Properties load(String path) throws IOException {
		Properties props = new Properties();
		props.load(ConfigUtils.class.getResourceAsStream(path));

		return props;
	}
	
	public static boolean getBoolean(Properties props, String key) {
		return Boolean.parseBoolean(props.getProperty(key).trim());
	}
	
	public static boolean getBoolean(Properties props, String key, boolean def) {
		return props.containsKey(key) ?
			Boolean.parseBoolean(props.getProperty(key).trim()) :
			def;
	}
	
	public static int getInt(Properties props, String key) {
		return Integer.parseInt(props.getProperty(key).trim());
	}
	
	public static long getLong(Properties props, String key) {
		return Long.parseLong(props.getProperty(key).trim());
	}
	
	public static InetAddress getInetAddress(Properties props, String key) throws UnknownHostException {
		return InetAddress.getByName(props.getProperty(key).trim());
	}
	
	public static InetSocketAddress getInetSocketAddress(Properties props, String addressKey, String portKey) {
		return new InetSocketAddress(props.getProperty(addressKey).trim(), getInt(props, portKey));
	}
	
	public static byte[] getBytes(Properties props, String key) {
		return Base64.getDecoder().decode(props.getProperty(key).trim());
	}
	
	public static String[] getStrings(Properties props, String key) {
		return props.getProperty(key).trim().split(",");
	}
	
}