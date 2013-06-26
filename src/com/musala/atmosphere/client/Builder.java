package com.musala.atmosphere.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.log4j.Logger;

import com.musala.atmosphere.client.exceptions.DeviceNotFoundException;
import com.musala.atmosphere.client.exceptions.MissingServerAnnotationException;
import com.musala.atmosphere.client.exceptions.ServerConnectionFailedException;
import com.musala.atmosphere.client.util.Server;
import com.musala.atmosphere.commons.Pair;
import com.musala.atmosphere.commons.cs.RmiStringConstants;
import com.musala.atmosphere.commons.cs.clientbuilder.DeviceParameters;
import com.musala.atmosphere.commons.cs.clientbuilder.IClientBuilder;
import com.musala.atmosphere.commons.cs.clientdevice.IClientDevice;

/**
 * Used by the user to get appropriate device in the server's pool.
 * 
 * @author vladimir.vladimirov
 */
public class Builder
{
	private static final Logger LOGGER = Logger.getLogger(Builder.class.getCanonicalName());

	private static Builder builder = null;

	private static String serverIp;

	private static int serverRmiPort;

	private IClientBuilder clientBuilder;

	private Registry serverRmiRegistry;

	/**
	 * Connects to Server through given IP and rmiPort.
	 * 
	 * @param annotationServerIp
	 * @param annotationRmiPort
	 * @throws RuntimeException
	 */
	private Builder(String annotationServerIp, int annotationRmiPort)
	{
		serverIp = annotationServerIp;
		serverRmiPort = annotationRmiPort;

		try
		{
			serverRmiRegistry = LocateRegistry.getRegistry(annotationServerIp, annotationRmiPort);
			clientBuilder = (IClientBuilder) serverRmiRegistry.lookup(RmiStringConstants.POOL_MANAGER.toString());
		}
		catch (RemoteException e)
		{
			LOGGER.fatal("Cannot connect to RMI. There is no RMI connection to IP: \"" + annotationServerIp
					+ "\" on port: \"" + annotationRmiPort + "\"", e);
			throw new ServerConnectionFailedException("Cannot connect to RMI. There is no RMI connection to IP: \""
					+ annotationServerIp + "\" on port: \"" + annotationRmiPort + "\"");
		}
		catch (NotBoundException e)
		{
			LOGGER.fatal(	"There is no POOL_MANAGER registered in RMI. You are connecting to something different than a Server.",
							e);
			throw new ServerConnectionFailedException("You are trying to connect to something that is not a Server.");
		}

		LOGGER.info("Builder has connected to the server's Pool of devices.");
	}

	/**
	 * Gets server IP and Port from the <i>@Server</i> annotation of the test class or throws
	 * MissingServerAnnotationException at runtime.
	 * 
	 * @return Pair of type (String, Integer) in the context of (IP, port)
	 */
	private static Pair<String, Integer> reflectServerAnnotationValues()
	{
		String serverIp = null;
		Integer serverPort = null;

		Exception exception = new Exception();
		StackTraceElement[] callerMethods = exception.getStackTrace();

		for (StackTraceElement callerMethod : callerMethods)
		{
			// going up in the stack trace to see which class has annotation Server
			Class<?> callerClass = null;
			try
			{
				callerClass = Class.forName(callerMethod.getClassName());
				if (callerClass.isAnnotationPresent(Server.class))
				{
					Server serverAnnotation = (Server) callerClass.getAnnotation(Server.class);
					serverIp = serverAnnotation.ip();
					serverPort = serverAnnotation.port();
					break;
				}
			}
			catch (ClassNotFoundException e)
			{
				LOGGER.error("Could not find class with name: " + callerMethod.getClassName());
			}

		}

		if (serverIp == null || serverPort == null)
		{
			LOGGER.fatal("@Server annotation missing on Test class.");
			throw new MissingServerAnnotationException("@Server annotation missing on Test class.");
		}

		Pair<String, Integer> annotationValues = new Pair<String, Integer>(serverIp, serverPort);
		return annotationValues;
	}

	/**
	 * Gets the instance of the builder
	 * 
	 * @return Instance of the builder
	 */
	public static Builder getInstance()
	{
		if (builder == null)
		{
			synchronized (Builder.class)
			{
				// Getting the server IP/port from the annotation
				Pair<String, Integer> annotationPair = reflectServerAnnotationValues();

				String reflectedServerIp = annotationPair.getKey();
				Integer reflectedRmiPort = annotationPair.getValue();

				if (builder == null)
				{
					builder = new Builder(reflectedServerIp, reflectedRmiPort);
					LOGGER.info("Builder instance has been created.");
				}

			}
		}

		return builder;
	}

	/**
	 * Gets Device with given ClientDeviceParameters.
	 * 
	 * @param deviceParameters
	 * @return
	 */
	public Device getDevice(DeviceParameters deviceParameters)
	{
		Device device = null;

		try
		{
			String deviceProxyRmiId = clientBuilder.getDeviceProxyRmiId(deviceParameters);
			LOGGER.info(deviceProxyRmiId);
			IClientDevice iClientDevice = (IClientDevice) serverRmiRegistry.lookup(deviceProxyRmiId);
			device = new Device(iClientDevice);
		}
		catch (RemoteException e)
		{
			LOGGER.error("Could not instantiate Device.", e);
			throw new ServerConnectionFailedException("Could not contact Server to retrieve device.", e);
		}
		catch (NotBoundException e)
		{
			LOGGER.error("No device found with given ID in RMI.", e);
			throw new DeviceNotFoundException("No device with given ID is present in RMI.", e);
		}

		return device;
	}

	public String getServerIp()
	{
		return serverIp;
	}

	public int getServerRmiPort()
	{
		return serverRmiPort;
	}
}
