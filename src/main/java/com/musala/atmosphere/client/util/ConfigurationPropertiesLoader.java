package com.musala.atmosphere.client.util;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.musala.atmosphere.client.exceptions.InvalidPropertyValueExceptipon;
import com.musala.atmosphere.commons.util.PropertiesLoader;

/**
 * Reads properties from the test project properties config file.
 *
 * @author dimcho.nedev
 *
 */
public class ConfigurationPropertiesLoader {
    private static final String CONFIG_PROPERTIES = "config.properties";

    private final static Logger LOGGER = Logger.getLogger(ConfigurationPropertiesLoader.class.getCanonicalName());

    /**
     * Gets the desired property from the config file in String type.
     *
     * @param property
     *        - the configuration property to be returned.
     * @return the desired property value.
     */
    private synchronized static String getPropertyString(ConfigurationProperties property) {
        PropertiesLoader propertiesLoader = PropertiesLoader.getInstance(CONFIG_PROPERTIES);
        String propertyString = property.toString();
        String resultProperty = propertiesLoader.getPropertyString(propertyString);

        return resultProperty;
    }

    /**
     * Gets the IP address of the Server.
     *
     * @return IP address of the Server
     */
    public static String getServerIp() {
        String serverIp = getPropertyString(ConfigurationProperties.SERVER_IP).trim();
        validatePropertyValue(serverIp, ConfigurationProperties.SERVER_IP);

        return getPropertyString(ConfigurationProperties.SERVER_IP);
    }

    /**
     * Gets the port where the Server is listening.
     *
     * @return the Server port number
     */
    public static int getServerPort() {
        String serverPortValue = getPropertyString(ConfigurationProperties.SERVER_PORT);
        validatePropertyValue(serverPortValue, ConfigurationProperties.SERVER_PORT);

        return Integer.parseInt(serverPortValue);
    }

    /**
     * Gets the connection retries count.
     *
     * @return connection retries count
     */
    public static int getConnectionRetries() {
        String connectionRetriesValue = getPropertyString(ConfigurationProperties.SERVER_CONNECTION_RETRIES).trim();
        validatePropertyValue(connectionRetriesValue, ConfigurationProperties.SERVER_CONNECTION_RETRIES);

        return Integer.parseInt(connectionRetriesValue);
    }

    /**
     * Returns whether the FTP server is enabled in the configuration file.
     *
     * @return <code>true</code> if the FTP server is enabled, otherwise returns <code>false</code>
     */
    public static boolean hasFtpServer() {
        String hasFtpServerValue = getPropertyString(ConfigurationProperties.FTP_SERVER).trim();
        return Boolean.parseBoolean(hasFtpServerValue);
    }

    /**
     * Gets the remote upload directory on the FTP server where the screen records will be uploaded.
     *
     * @return remote upload directory name
     */
    public static String getFtpRemoteUplaodDirectory() {
        String uploadDirectoryName = getPropertyString(ConfigurationProperties.FTP_DIR).trim();
        validatePropertyValue(uploadDirectoryName, ConfigurationProperties.FTP_DIR);

        Pattern pattern = Pattern.compile("^[^\\/?%*:|\"<>]+$");
        Matcher matcher = pattern.matcher(uploadDirectoryName);
        boolean isMatch = matcher.matches();
        if (!isMatch) {
            String errorMessage = String.format("%s has invalid directory name property: %s",
                                                ConfigurationProperties.FTP_DIR,
                                                uploadDirectoryName);
            LOGGER.error(errorMessage);
            throw new InvalidPropertyValueExceptipon(errorMessage);
        }

        return uploadDirectoryName;
    }

    /**
     * Returns whether the config.properties file exist in the test project working directory.
     *
     * @return <code>true</code> if the config.properties file exist, otherwise returns <code>false</code>
     */
    public static boolean isConfigExists() {
        boolean exists = false;
        File configfile = new File(CONFIG_PROPERTIES);

        if (configfile.exists() && !configfile.isDirectory()) {
            exists = true;
        }

        return exists;
    }

    private static void validatePropertyValue(String connectionRetriesValue, ConfigurationProperties propertyType) {
        if (connectionRetriesValue.isEmpty()) {
            String errorMessage = String.format("%s value cannot be empty.", propertyType);
            LOGGER.error(errorMessage);
            throw new InvalidPropertyValueExceptipon(errorMessage);
        }
    }

}
