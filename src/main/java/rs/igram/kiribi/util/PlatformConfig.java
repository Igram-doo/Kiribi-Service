package rs.igram.kiribi.service.util;

import java.io.IOException;
import java.util.Properties;

import static rs.igram.kiribi.service.util.ConfigUtils.load;

public class PlatformConfig {
	public static final String homeDir;
	
	static{
		try{
			homeDir = System.getProperties().containsKey("user.home") ?
				System.getProperty("user.home") :
				load("/platform.config").getProperty("user.home"); 	
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Could not determine home dir",e);
		}
	}

	private PlatformConfig() {}
}