package com.musala.atmosphere.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.rmi.RemoteException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.musala.atmosphere.client.exceptions.DeviceReleasedException;
import com.musala.atmosphere.commons.PowerProperties;
import com.musala.atmosphere.commons.ScreenOrientation;
import com.musala.atmosphere.commons.SmsMessage;
import com.musala.atmosphere.commons.beans.DeviceAcceleration;
import com.musala.atmosphere.commons.beans.DeviceOrientation;
import com.musala.atmosphere.commons.beans.PhoneNumber;
import com.musala.atmosphere.commons.cs.clientdevice.IClientDevice;

/**
 *
 * @author yordan.petrov
 *
 */
public class ReconnectDeviceTest
{
	private static IClientDevice mockedClientDevice;

	private static ServerConnectionHandler mockedServerConnectionHandler;

	private static Device testDevice;

	@BeforeClass
	public static void setUp() throws Exception
	{
		mockedClientDevice = mock(IClientDevice.class);
		doThrow(new RemoteException()).when(mockedClientDevice).getPowerProperties(anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).initApkInstall(anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).executeShellCommand(anyString(), anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).getUiXml(anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).getConnectionType(anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).getDeviceAcceleration(anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).getDeviceOrientation(anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).getDeviceInformation(anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).getMobileDataState(anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).getScreenshot(anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).setAcceleration(any(DeviceAcceleration.class),
																				anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).setDeviceOrientation(	any(DeviceOrientation.class),
																						anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).setPowerProperties(	any(PowerProperties.class),
																					anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).setWiFi(anyBoolean(), anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).receiveSms((SmsMessage) any(), anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).receiveCall((PhoneNumber) any(), anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).acceptCall((PhoneNumber) any(), anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).holdCall((PhoneNumber) any(), anyLong());
		doThrow(new RemoteException()).when(mockedClientDevice).cancelCall((PhoneNumber) any(), anyLong());

		mockedServerConnectionHandler = mock(ServerConnectionHandler.class);

		testDevice = new Device(mockedClientDevice, 0, mockedServerConnectionHandler);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnGetBatteryLevel()
	{
		testDevice.getPowerProperties();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnAppendToApk()
	{
		testDevice.installAPK("");
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnGetActiveScreen()
	{
		testDevice.getActiveScreen();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnGetConnectionType()
	{
		testDevice.getConnectionType();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnGetDeviceAcceleration()
	{
		testDevice.getDeviceAcceleration();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnGetDeviceOrientation()
	{
		testDevice.getDeviceOrientation();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnGetInformation()
	{
		testDevice.getInformation();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnGetMobileDataState()
	{
		testDevice.getMobileDataState();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnGetScreenshot()
	{
		testDevice.getScreenshot();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnGetScreenshotWithPath()
	{
		testDevice.getScreenshot("./");
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnInputText()
	{
		testDevice.inputText("asd", 0);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnIsAwake()
	{
		testDevice.isAwake();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnIsLocked()
	{
		testDevice.isLocked();
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnLock()
	{
		testDevice.setLocked(true);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnPressButton()
	{
		testDevice.pressButton(0);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnSetAcceleration()
	{
		testDevice.setAcceleration(new DeviceAcceleration());
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnSetorientation()
	{
		testDevice.setDeviceOrientation(new DeviceOrientation());
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnSetAirplaneMode()
	{
		testDevice.setAirplaneMode(true);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnSetAutoRotation()
	{
		testDevice.setAutoRotation(true);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnSetBatteryState()
	{
		testDevice.setPowerProperties(new PowerProperties());
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnSetScreenOrientation()
	{
		testDevice.setScreenOrientation(ScreenOrientation.LANDSCAPE);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnSetWiFi()
	{
		testDevice.setWiFi(true);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnUnlock()
	{
		testDevice.setLocked(false);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnReceiveSms()
	{
		PhoneNumber phoneNumber = new PhoneNumber("123");
		SmsMessage smsMessage = new SmsMessage(phoneNumber, "");
		testDevice.receiveSms(smsMessage);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnReceiveCall()
	{
		PhoneNumber phoneNumber = new PhoneNumber("123");
		testDevice.receiveCall(phoneNumber);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnAcceptCall()
	{
		PhoneNumber phoneNumber = new PhoneNumber("123");
		testDevice.acceptCall(phoneNumber);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnHoldCall()
	{
		PhoneNumber phoneNumber = new PhoneNumber("123");
		testDevice.holdCall(phoneNumber);
	}

	@Test(expected = DeviceReleasedException.class)
	public void testThrowsExceptionOnCancelCall()
	{
		PhoneNumber phoneNumber = new PhoneNumber("123");
		testDevice.cancelCall(phoneNumber);
	}
}