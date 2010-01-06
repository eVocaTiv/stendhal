package games.stendhal.server.core.engine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;

import marauroa.common.crypto.RSAKey;

public class GenerateINI {

	/** The name of the output file. */
	static final String FILENAME = "server.ini";

	
	/** Where data is read from. */
	private static BufferedReader in = new BufferedReader(
			new InputStreamReader(System.in));
	private static String gameName;

	private static String databaseName;

	private static String databaseHost;

	private static String databaseUsername;

	private static String databasePassword;

	private static String databaseImplementation;

	private static String tcpPort;

	private static String worldImplementation;

	private static String ruleprocessorImplementation;

	private static String turnLength;

	private static String statisticsFilename;

	private static RSAKey rsakey;

	
	/**
	 * reads a String from the input. When no String is chosen the defaultValue
	 * is used.
	 * 
	 * @param input
	 *            the buffered input, usually System.in
	 * @param defaultValue
	 *            if no value is written.
	 * @return the string read or default if none was read.
	 */
	public static String getStringWithDefault(final BufferedReader input,
			final String defaultValue) {
		String ret = "";
		try {
			ret = input.readLine();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		if (ret != null) {
			if ((ret.length() == 0) && (defaultValue != null)) {
				ret = defaultValue;
			}
		}
		return ret;

	}

	/**
	 * reads a String from the input. When no String is choosen the errorMessage
	 * is is displayed and the application is terminated.
	 * 
	 * @param input
	 *            the input stream, usually System.in
	 * @param errorMessage
	 *            the error message to print when failing
	 * @return string read from input
	 */
	public static String getStringWithoutDefault(final BufferedReader input,
			final String errorMessage) {
		String ret = "";
		try {
			ret = input.readLine();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if ((ret == null) || (ret.length() == 0)) {
			System.out.println(errorMessage);
			System.out.println("Terminating...");
			System.exit(1);
		}
		return ret;
	}

	/**
	 * Makes the first letter of the source uppercase.
	 * 
	 * @param source
	 *            the string
	 * @return *T*he string, with first letter is upper case.
	 */
	public static String uppcaseFirstLetter(final String source) {
		if (source.length() > 0) {
			return Character.toUpperCase(source.charAt(0))
					+ source.substring(1);
		}
		return source;
	}


	public static void main(final String[] args) throws FileNotFoundException {
		gameName = "stendhal";

		/** Write configuration for database */
		databaseImplementation = getDatabaseImplementation();
		databaseName = getDatabaseName();
		databaseHost = getDatabaseHost();
		databaseUsername = getDatabaseUsername();
		databasePassword = getDatabasePassword();

		System.out.println("Using \"" + databaseName + "\" as database name\n");
		System.out.println("Using \"" + databaseHost + "\" as database host\n");
		System.out.println("Using \"" + databaseUsername
				+ "\" as database user\n");
		System.out.println("Using \"" + databasePassword
				+ "\" as database user password\n");

		System.out.println("In order to make these options effective please run:");
		System.out.println("# mysql");
		System.out.println("  create database " + databaseName + ";");
		System.out.println("  grant all on " + databaseName + ".* to "
				+ databaseUsername + "@localhost identified by '"
				+ databasePassword + "';");
		System.out.println("  exit");

		tcpPort = getTCPPort();

		worldImplementation = getWorldImplementation();
		ruleprocessorImplementation = getRuleProcessorImplementation();

		turnLength = getTurnLength();

		statisticsFilename = getStatisticsFilename();

		/* The size of the RSA Key in bits, usually 512 */
		final String keySize = getRSAKeyBits();
		System.out.println("Using key of " + keySize + " bits.");
		System.out.println("Please wait while the key is generated.");
		rsakey = RSAKey.generateKey(Integer.valueOf(keySize));
		final PrintWriter out = new PrintWriter(new FileOutputStream(FILENAME));
		write(out);
		out.close();

		System.out.println(FILENAME + " has been generated.");
	}

	private static String getRSAKeyBits() {
		System.out.print("Write size for the RSA key of the server. Be aware that a key bigger than 1024 could be very long to create [512]: ");
		final String keySize = getStringWithDefault(in, "512");
		return keySize;
	}

	private static String getStatisticsFilename() {
		return "./server_stats.xml";
	}

	private static String getTurnLength() {
		return "300";
	}

	private static String getRuleProcessorImplementation() {
		return "games.stendhal.server.core.engine.StendhalRPRuleProcessor";
	}

	private static String getWorldImplementation() {
		return "games.stendhal.server.core.engine.StendhalRPWorld";
	}

	private static String getTCPPort() {
		return "32160";
	}

	private static String getDatabaseImplementation() {
		return "games.stendhal.server.core.engine.StendhalPlayerDatabase";
	}

	private static void write(final PrintWriter out) {
		out.println("# Generated .ini file for Test Game at " + new Date());
		out.println("# Database and factory classes. Don't edit.");
		out.println("database_implementation=" + databaseImplementation);
		out.println("factory_implementation=games.stendhal.server.core.engine.StendhalRPObjectFactory");
		out.println();
		out.println("# Database information. Edit to match your configuration.");
		out.println("jdbc_url=jdbc:mysql://" + databaseHost + "/"
				+ databaseName);
		out.println("jdbc_class=com.mysql.jdbc.Driver");
		out.println("jdbc_user=" + databaseUsername);
		out.println("jdbc_pwd=" + databasePassword);
		out.println();
		out.println("# TCP port stendhald will use. ");
		out.println("tcp_port=" + tcpPort);
		out.println();
		out.println("# World and RP configuration. Don't edit.");
		out.println("world=" + worldImplementation);
		out.println("ruleprocessor=" + ruleprocessorImplementation);
		out.println();
		out.println("turn_length=" + turnLength);
		out.println();
		out.println("server_typeGame=" + gameName);
		out.println("server_name=" + gameName + " Marauroa server");
		out.println("server_version=0.80");
		out.println("server_contact=https://sourceforge.net/tracker/?atid=514826&group_id=66537&func=browse");
		out.println();
		out.println("# Extensions configured on the server. Enable at will.");
		out.println("#server_extension=groovy,http");
		out.println("#groovy=games.stendhal.server.scripting.StendhalGroovyRunner");
		out.println("#http=games.stendhal.server.extension.StendhalHttpServer");
		out.println("#http.port=8080");
		out.println();
		out.println("statistics_filename=" + statisticsFilename);
		out.println();
		rsakey.print(out);
	}

	protected static String getDatabasePassword() {
		System.out.print("Write value of the database user password: ");
		final String databasepassword = getStringWithoutDefault(in,
				"Please enter a database password");
		return databasepassword;
	}

	protected static String getDatabaseUsername() {
		System.out.print("Write name of the database user: ");
		final String databaseuser = getStringWithoutDefault(in,
				"Please enter a database user");
		return databaseuser;
	}

	protected static String getDatabaseHost() {
		System.out.print("Write name of the database host [localhost]: ");
		final String databasehost = getStringWithDefault(in, "localhost");
		return databasehost;
	}

	protected static String getDatabaseName() {
		System.out.print("Write name of the database [marauroa]: ");
		final String databasename = getStringWithDefault(in, "marauroa");
		return databasename;
	}
}
