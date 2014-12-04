package conf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class ConfigMgr {
	private static HashMap<String, String> parameters = new HashMap<String, String>();

	static {
		BufferedReader br = null;
		try {
			String currentDirPath = System.getProperty("user.dir");
			String confFileName = "config.txt";
			File currentDir = new File(currentDirPath);
			File configFile = new File(currentDir, confFileName);

			for (int i = 0; i < 3; i++) {
				if (configFile.exists())
					break;
				currentDir = currentDir.getParentFile();
				configFile = new File(currentDir, confFileName);
			}

			FileReader reader = new FileReader(configFile);
			br = new BufferedReader(reader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		try {
			while (br.ready()) {
				String line = br.readLine();

				if (!line.startsWith("#")) {
					StringTokenizer tk = new StringTokenizer(line, "\"=");

					if (tk.countTokens() >= 2) {
						parameters.put(tk.nextToken().toLowerCase(),
								tk.nextToken());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static String readConfigurationParameter(String parameterName) {
		return parameters.get(parameterName.toLowerCase());
	}

	public static void setConfigurationParameter(String parameterName,
			String parameterValue) {
		parameters.put(parameterName.toLowerCase(), parameterValue);
	}

	public static void main(String args[]) {
		for (String param : ConfigMgr.parameters.keySet()) {
			System.out
					.println(param + ": " + readConfigurationParameter(param));
		}

	}
}
