package com.musala.atmosphere.client;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import com.musala.atmosphere.client.device.HardwareButton;
import com.musala.atmosphere.client.device.log.LogCatLevel;
import com.musala.atmosphere.client.entity.AccessibilityElementEntity;
import com.musala.atmosphere.client.entity.DeviceSettingsEntity;
import com.musala.atmosphere.client.entity.GestureEntity;
import com.musala.atmosphere.client.entity.GpsLocationEntity;
import com.musala.atmosphere.client.entity.HardwareButtonEntity;
import com.musala.atmosphere.client.entity.ImageEntity;
import com.musala.atmosphere.client.entity.ImeEntity;
import com.musala.atmosphere.client.exceptions.ActivityStartingException;
import com.musala.atmosphere.client.exceptions.GettingScreenshotFailedException;
import com.musala.atmosphere.client.util.ClientConstants;
import com.musala.atmosphere.client.util.ConfigurationPropertiesLoader;
import com.musala.atmosphere.client.util.LogcatAnnotationProperties;
import com.musala.atmosphere.client.util.settings.DeviceSettingsManager;
import com.musala.atmosphere.commons.ConnectionType;
import com.musala.atmosphere.commons.DeviceInformation;
import com.musala.atmosphere.commons.PowerProperties;
import com.musala.atmosphere.commons.RoutingAction;
import com.musala.atmosphere.commons.ScreenOrientation;
import com.musala.atmosphere.commons.SmsMessage;
import com.musala.atmosphere.commons.TelephonyInformation;
import com.musala.atmosphere.commons.beans.DeviceAcceleration;
import com.musala.atmosphere.commons.beans.DeviceMagneticField;
import com.musala.atmosphere.commons.beans.DeviceOrientation;
import com.musala.atmosphere.commons.beans.DeviceProximity;
import com.musala.atmosphere.commons.beans.MobileDataState;
import com.musala.atmosphere.commons.beans.PhoneNumber;
import com.musala.atmosphere.commons.beans.SwipeDirection;
import com.musala.atmosphere.commons.connectivity.WifiConnectionProperties;
import com.musala.atmosphere.commons.exceptions.CommandFailedException;
import com.musala.atmosphere.commons.geometry.Point;
import com.musala.atmosphere.commons.gesture.Gesture;
import com.musala.atmosphere.commons.util.GeoLocation;
import com.musala.atmosphere.commons.util.IntentBuilder;
import com.musala.atmosphere.commons.util.IntentBuilder.IntentAction;
import com.musala.atmosphere.commons.util.Pair;

/**
 * Android device representing class.
 *
 * @author vladimir.vladimirov
 *
 */
public class Device {
    private static final int MAX_BUFFER_SIZE = 8092; // 8K

    private static final Logger LOGGER = Logger.getLogger(Device.class.getCanonicalName());

    private static final String ATMOSPHERE_SERVICE_PACKAGE = "com.musala.atmosphere.service";

    private static final String ATMOSPHERE_UNLOCK_DEVICE_ACTIVITY = ".UnlockDeviceActivity";

    private static final int WAIT_FOR_AWAKE_STATE_INTERVAL = 100;

    /**
     * Default timeout for the hold phase from long click gesture. It needs to be more than the system long click
     * timeout which varies from device to device, but is usually around 1 second.
     */
    public static final int LONG_PRESS_DEFAULT_TIMEOUT = 1500; // ms

    private static final String LOCAL_DIR = System.getProperty("user.dir");

    private final DeviceCommunicator communicator;

    private GestureEntity gestureEntity;

    private HardwareButtonEntity hardwareButtonEntity;

    private ImeEntity imeEntity;

    private DeviceSettingsEntity settingsEntity;

    private ImageEntity imageEntity;

    private AccessibilityElementEntity elementEntity;

    private GpsLocationEntity gpsLocationEntity;

    private boolean isLogcatEnabled = true;

    private String screenRecordUploadDiectoryName;

    private boolean isScreenRecordingStarted = false;

    /**
     * Constructor that creates a usable Device object by a given {@link DeviceCommunicator device communicator}.
     *
     * @param deviceCommunicator
     */
    Device(DeviceCommunicator deviceCommunicator) {
        this.communicator = deviceCommunicator;
    }

    /**
     * Accepts call to this device.
     *
     * @return <code>true</code> if the accepting call is successful, <code>false</code> if it fails.
     */
    public boolean acceptCall() {
        return pressButton(HardwareButton.ANSWER);
    }

    /**
     * Accepts a call to this device.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @param phoneNumber
     *        - {@link PhoneNumber}, that calls the device.
     * @return <code>true</code> if the accepting call is successful, <code>false</code> if it fails.
     */
    public boolean acceptCall(PhoneNumber phoneNumber) {
        Object result = communicator.sendAction(RoutingAction.CALL_ACCEPT, phoneNumber);
        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Cancels a call to this device.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @param phoneNumber
     *        - {@link PhoneNumber}, that calls the device.
     * @return <code>true</code> if the canceling call is successful, <code>false</code> if it fails.
     */
    public boolean cancelCall(PhoneNumber phoneNumber) {
        Object result = communicator.sendAction(RoutingAction.CALL_CANCEL, phoneNumber);
        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Declines a call to this device.<br>
     *
     * @return <code>true</code> if the denying call is successful, <code>false</code> if it fails.
     */
    public boolean declineCall() {
        return pressButton(HardwareButton.DECLINE);
    }

    /**
     * Executes a command in the shell of this device.
     *
     * @param shellCommand
     *        - String, representing the command for execution.
     * @return the output of this device console, after the command is executed.
     */
    public String executeShellCommand(String shellCommand) {
        String result = (String) communicator.sendAction(RoutingAction.EXECUTE_SHELL_COMMAND, shellCommand);
        return result;
    }

    /**
     * Executes a command in the shell of this device in a new thread.
     *
     * @param shellCommand
     *        - command to be executed in background
     */
    private void executeShellCommandInBackground(String shellCommand) {
        // TODO: Remove this method.
        communicator.sendAction(RoutingAction.EXECUTE_SHELL_COMMAND_IN_BACKGROUND, shellCommand);
    }

    /**
     * Interrupts a background executing shell process.
     *
     * @param processName
     *        - name of the process to be interrupted
     */
    private void interruptBackgroundShellProcess(String processName) {
        // TODO: Remove this method.
        communicator.sendAction(RoutingAction.INTERRUPT_BACKGROUND_SHELL_PROCESS, processName);
    }

    /**
     * Installs a specified Android application file on this device.<br>
     *
     * @param path
     *        - location of the file to be installed.
     * @return <code>true</code> if the APK installation is successful, <code>false</code> if it fails.
     */
    private boolean doApkInstallation(String path, boolean shouldForceInstall) {
        // A string that will be used to tell which step of installation was
        // reached
        String currentInstallationStepDescription = null;
        FileInputStream fileReaderFromApk = null;
        try {
            currentInstallationStepDescription = "Create file for storing the apk";
            LOGGER.info(currentInstallationStepDescription);
            Object response = communicator.sendAction(RoutingAction.APK_INIT_INSTALL);
            if (response != DeviceCommunicator.VOID_SUCCESS) {
                throw communicator.getLastException();
            }

            currentInstallationStepDescription = "Locating the file to store the apk in";
            LOGGER.info(currentInstallationStepDescription);
            // Transfer the installation file from the current machine to the
            // device
            byte[] buffer = new byte[MAX_BUFFER_SIZE];
            fileReaderFromApk = new FileInputStream(path);

            currentInstallationStepDescription = "Transferring installation file";
            LOGGER.info(currentInstallationStepDescription);
            int readBytes;
            while ((readBytes = fileReaderFromApk.read(buffer)) >= 0) {
                response = communicator.sendAction(RoutingAction.APK_APPEND_DATA, buffer, readBytes);
                if (response != DeviceCommunicator.VOID_SUCCESS) {
                    throw communicator.getLastException();
                }
            }

            currentInstallationStepDescription = "Installing transferred file";
            LOGGER.info(currentInstallationStepDescription);
            response = communicator.sendAction(RoutingAction.APK_BUILD_AND_INSTALL, shouldForceInstall);
            if (response != DeviceCommunicator.VOID_SUCCESS) {
                throw communicator.getLastException();
            }
            String message = "File installation successfull.";
            LOGGER.info(message);
        } catch (IOException | CommandFailedException e) {
            String message = String.format("Exception occurred while '%s'.", currentInstallationStepDescription);
            LOGGER.fatal(message, e);
            // This method should work even if the apk file was not created at
            // all.
            communicator.sendAction(RoutingAction.APK_DISCARD);
            return false;
        } finally {
            if (fileReaderFromApk != null) {
                try {
                    fileReaderFromApk.close();
                } catch (IOException e) {
                    // Nothing can be done here anymore
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Simulates a double tap on the specified point.
     *
     * @param point
     *        - the point to be tapped
     * @return <code>true</code> if the double tap is successful, <code>false</code> if it fails.
     */
    public boolean doubleTap(Point point) {
        return gestureEntity.doubleTap(point);
    }

    /**
     * Executes user-described gesture on this device.
     *
     * @param gesture
     *        - the gesture to be executed.
     */
    public void executeGesture(Gesture gesture) {
        communicator.sendAction(RoutingAction.PLAY_GESTURE, gesture);
    }

    /**
     * Gets the currently active {@link Screen Screen} of this device.
     *
     * @return {@link Screen} instance, representing the active screen of this device or <code>null</code> if getting
     *         active screen fails.
     */
    public Screen getActiveScreen() {
        return new Screen(gestureEntity, imeEntity, settingsEntity, imageEntity, elementEntity, communicator);
    }

    /**
     * Gets the airplane mode state of this device.<br>
     *
     * @return <code>true</code> if the airplane mode is on, <code>false</code> if it's off and <code>null</code> if
     *         getting airplane mode fails.
     */
    public Boolean getAirplaneMode() {
        return settingsEntity.getAirplaneMode();
    }

    DeviceCommunicator getCommunicator() {
        return communicator;
    }

    /**
     * Gets the current network connection type of this device.
     *
     * @return the {@link ConnectionType type} of the network on this device, or <code>null</code> if getting connection
     *         type fails.
     * @see ConnectionType
     *
     */
    public ConnectionType getConnectionType() {
        ConnectionType type = (ConnectionType) communicator.sendAction(RoutingAction.GET_CONNECTION_TYPE);
        return type;
    }

    /**
     * Gets current acceleration of this device.
     *
     * @return the movement {@link DeviceAcceleration vector} of this device in the space or <code>null</code> if
     *         getting acceleration fails.
     * @see DeviceAcceleration
     */
    public DeviceAcceleration getDeviceAcceleration() {
        DeviceAcceleration deviceAcceleration = (DeviceAcceleration) communicator.sendAction(RoutingAction.GET_DEVICE_ACCELERATION);
        return deviceAcceleration;
    }

    /**
     * Gets the current proximity of the device.
     *
     * @return a float representing the proximity of the device or null if the getting of the proximity failed
     */
    public float getDeviceProximity() {
        float proximity = (float) communicator.sendAction(RoutingAction.GET_DEVICE_PROXIMITY);

        return proximity;
    }

    /**
     * Gets current orientation in space of this device.
     *
     * @return {@link DeviceOrientation DeviceOrientation} of the testing device,<br>
     *         <code>null</code> if getting device orientation fails.
     */
    public DeviceOrientation getDeviceOrientation() {
        DeviceOrientation deviceOrientation = (DeviceOrientation) communicator.sendAction(RoutingAction.GET_DEVICE_ORIENTATION);
        return deviceOrientation;
    }

    /**
     * Provides information about device physical properties, such as type (tablet or emulator), dpi, resolution,
     * android API level, manufacturer, camera presence and others.
     *
     * @return {@link DeviceInformation DeviceInformation} structure with information for the testing device,<br>
     *         <code>null</code> if getting device information fails.
     */
    public DeviceInformation getInformation() {
        DeviceInformation wrappedDeviceInformation = (DeviceInformation) communicator.sendAction(RoutingAction.GET_DEVICE_INFORMATION);
        return wrappedDeviceInformation;
    }

    /**
     * Gets the current mobile data state of this device.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @return the {@link MobileDataState state} of mobile data on this device or <code>null</code> if getting mobile
     *         data state fails.
     * @see MobileDataState
     */
    public MobileDataState getMobileDataState() {
        MobileDataState state = (MobileDataState) communicator.sendAction(RoutingAction.GET_MOBILE_DATA_STATE);
        return state;
    }

    /**
     * Gets a {@link PowerProperties} instance that contains information about the current device power-related
     * environment.
     *
     * @return a filled {@link PowerProperties} instance (or <code>null</code> if fetching the environment fails).
     */
    public PowerProperties getPowerProperties() {
        PowerProperties result = (PowerProperties) communicator.sendAction(RoutingAction.GET_POWER_PROPERTIES);
        return result;
    }

    /**
     * Gets screenshot of this device's active screen.
     *
     * @return byte buffer, containing captured device screen,<br>
     *         <code>null</code> if getting screenshot fails.<br>
     *         It can be subsequently dumped to a file and directly opened as a PNG image.
     */
    public byte[] getScreenshot() {
        return imageEntity.getScreenshot();
    }

    /**
     * Gets screenshot of this device's active screen and saves it as an image file at a specified location.
     *
     * @param pathToImageFile
     *        - location at which the screenshot image file should be saved.
     * @return <code>true</code> if the getting screenshot is successful, <code>false</code> if it fails.
     */
    public boolean getScreenshot(String pathToImageFile) {
        try {
            Path pathToPngFile = Paths.get(pathToImageFile);
            byte[] screenshot = getScreenshot();
            Files.write(pathToPngFile, screenshot);
        } catch (IOException e) {
            String message = "Saving the screenshot file failed.";
            LOGGER.error(message, e);
            return false;
        }

        return true;
    }

    /**
     * Gets a {@link ScreenOrientation} instance that contains information about the orientation of the screen.
     *
     * @return {@link ScreenOrientation object} that shows how android elements are rotated on the screen.
     * @see ScreenOrientation
     */
    public ScreenOrientation getScreenOrientation() {
        return settingsEntity.getScreenOrientation();
    }

    /**
     * Obtains information about the telephony services on the device.
     *
     * @return {@link TelephonyInformation} instance.
     */
    public TelephonyInformation getTelephonyInformation() {
        TelephonyInformation telephonyInformation = (TelephonyInformation) communicator.sendAction(RoutingAction.GET_TELEPHONY_INFO);
        return telephonyInformation;
    }

    /**
     * Returns device auto rotation state.
     *
     * @return <code>true</code> if the auto rotation is on , <code>false</code> if it's not,<code>null</code> if the
     *         method failed to get device auto rotation state.
     */
    public Boolean isAutoRotationOn() {
        return settingsEntity.isAutoRotationOn();
    }

    /**
     * Holds a call to this device.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @param phoneNumber
     *        - {@link PhoneNumber}, that calls the device.
     * @return <code>true</code> if the holding call is successful, <code>false</code> if it fails.
     */
    public boolean holdCall(PhoneNumber phoneNumber) {
        Object result = communicator.sendAction(RoutingAction.CALL_HOLD, phoneNumber);
        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Inputs text directly on the device, in the element on focus, if possible. It is user's responsibility to focus an
     * editable android widget using {@link Device#tapScreenLocation(Point) Device.tapScreenLocation()},
     * {@link UiElement#tap() UiElement.tap()} or {@link UiElement#focus() UiElement.focus()} methods.
     *
     * @param text
     *        - text to be input.
     * @return <code>true</code> if the text input is successful, <code>false</code> if it fails.
     */
    public boolean inputText(String text) {
        return inputText(text, 0);
    }

    /**
     * Simulates text typing in the element on focus for this device. It is user's responsibility to focus an editable
     * android widget using {@link Device#tapScreenLocation(Point) Device.tapScreenLocation()}, {@link UiElement#tap()
     * UiElement.tap()} or {@link UiElement#focus() UiElement.focus()} methods.
     *
     * @param text
     *        - text to be input.
     * @param interval
     *        - time interval in milliseconds between typing each symbol.
     * @return <code>true</code> if the text input is successful, <code>false</code> if it fails.
     */
    public boolean inputText(String text, long interval) {
        return imeEntity.inputText(text, interval);
    }

    /**
     * Clears the content of the focused text field.
     *
     * @return <code>true</code> if clear text is successful, <code>false</code> if it fails
     */
    public boolean clearText() {
        return imeEntity.clearText();
    }

    /**
     * Selects the content of the focused text field.
     *
     * @return <code>true</code> if the text selecting is successful, <code>false</code> if it fails
     */
    public boolean selectAllText() {
        return imeEntity.selectAllText();
    }

    /**
     * Paste a copied text in the current focused text field.
     *
     * @return <code>true</code> if the operation is successful, <code>false</code> if it fails
     */
    public boolean pasteText() {
        return imeEntity.pasteText();
    }

    /**
     * Copies the selected content of the focused text field.
     *
     * @return <code>true</code> if copy operation is successful, <code>false</code> if it fails
     */
    public boolean copyText() {
        return imeEntity.copyText();
    }

    /**
     * Cuts the selected text from the current focused text field.
     *
     * @return <code>true</code> if the operation is successful, <code>false</code> if it fails
     */
    public boolean cutText() {
        return imeEntity.cutText();
    }

    /**
     * Installs a specified Android application file on this device.<br>
     *
     * @param path
     *        - location of the file to be installed.
     * @return <code>true</code> if the APK installation is successful, <code>false</code> if it fails.
     */
    public boolean installAPK(String path) {
        return doApkInstallation(path, false);
    }

    /**
     * Installs a specified Android application file on this device.<br>
     *
     * @param path
     *        - location of the file to be installed.
     * @param shouldForceInstall
     *        - Indicates whether a force install should be performed
     * @return <code>true</code> if the APK installation is successful, <code>false</code> if it fails.
     */
    public boolean installAPK(String path, boolean shouldForceInstall) {
        return doApkInstallation(path, shouldForceInstall);
    }

    /**
     * Checks if this device is in a WAKE state.<br>
     *
     * @return <code>true</code> if the device is awake.<br>
     *         <code>false</code> if the device is asleep.<br>
     */
    public boolean isAwake() {
        boolean response = (boolean) communicator.sendAction(RoutingAction.GET_AWAKE_STATUS);
        return response;
    }

    /**
     * Checks if this device is locked.
     *
     * @return <code>true</code> if the device is locked.<br>
     *         <code>false</code> if the device is unlocked.
     */
    public Boolean isLocked() {
        return (boolean) communicator.sendAction(RoutingAction.IS_LOCKED);
    }

    /**
     * Simulates a pinch in having the initial coordinates of the fingers performing it.
     *
     * @param firstFingerInitial
     *        - the initial position of the first finger
     * @param secondFingerInitial
     *        - the initial position of the second finger
     * @return <code>true</code> if the pinch in is successful, <code>false</code> if it fails.
     */
    public boolean pinchIn(Point firstFingerInitial, Point secondFingerInitial) {
        return gestureEntity.pinchIn(firstFingerInitial, secondFingerInitial);
    }

    /**
     * Simulates a pinch out having the positions of the fingers performing it in the end of the gesture.
     *
     * @param firstFingerEnd
     *        - the position of the first finger in the end of the gesture
     * @param secondFingerEnd
     *        - the position of the second finger in the end of the gesture
     * @return <code>true</code> if the pinch out is successful, <code>false</code> if it fails.
     */
    public boolean pinchOut(Point firstFingerEnd, Point secondFingerEnd) {
        return gestureEntity.pinchOut(firstFingerEnd, secondFingerEnd);
    }

    /**
     * Presses hardware button on this device.
     *
     * @param button
     *        - {@link HardwareButton HardwareButton} to be pressed.
     * @return <code>true</code> if the button press is successful, <code>false</code> if it fails.
     */
    public boolean pressButton(HardwareButton button) {
        int keycode = button.getKeycode();
        return pressButton(keycode);
    }

    /**
     * Presses hardware button on this device.
     *
     * @param keyCode
     *        - button key code as specified by the Android KeyEvent KEYCODE_ constants.
     * @return <code>true</code> if the hardware button press is successful, <code>false</code> if it fails.
     */
    public boolean pressButton(int keyCode) {
        return hardwareButtonEntity.pressButton(keyCode);
    }

    /**
     * Simulates random finger actions on the screen of this device.
     *
     * @return <code>true</code> if the random multi-touch event execution is successful, <code>false</code> if it
     *         fails.
     */
    public boolean randomMultiTouchevent() {
        // TODO implement device.randomMultiTouchEvent()
        return false;
    }

    /**
     * This device receives a call.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @param phoneNumber
     *        - {@link PhoneNumber}, that will be sent to the device.
     * @return <code>true</code> if the call receiving is successful, <code>false</code> if it fails.
     */
    public boolean receiveCall(PhoneNumber phoneNumber) {
        Object result = communicator.sendAction(RoutingAction.CALL_RECEIVE, phoneNumber);
        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Sends SMS to this device.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @param smsMessage
     *        - {@link SmsMessage}, that will be sent to the device.
     * @return <code>true</code> if the SMS receiving is successful, <code>false</code> if it fails.
     */
    public boolean receiveSms(SmsMessage smsMessage) {
        Object result = communicator.sendAction(RoutingAction.SMS_RECEIVE, smsMessage);
        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Redirects specific IP address to another IP address.
     *
     * @param toIp
     *        - IP which will receive requests.
     * @param toNewIp
     *        - another IP to which the received requests from the first IP should be redirected.
     * @return <code>true</code> if the connection redirection is successful, <code>false</code> if it fails.
     */
    public boolean redirectConnection(String toIp, String toNewIp) {
        // TODO implement device.redirectConnection
        return false;
    }

    void release() {
        if(isScreenRecordingStarted) {
            stopScreenRecording();
        }

        if(isLogcatEnabled) {
            stopLogcat();
        }

        closeChromeDriver();
        communicator.release();
    }

    /**
     * Sets new acceleration for this device.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @param deviceAcceleration
     *        - new {@link DeviceAcceleration DeviceAcceleration} to be set.
     * @return <code>true</code> if the acceleration setting is successful, <code>false</code> if it fails.
     */
    public boolean setAcceleration(DeviceAcceleration deviceAcceleration) {
        Object result = communicator.sendAction(RoutingAction.SET_ACCELERATION, deviceAcceleration);
        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Sets the airplane mode state for this device.<br>
     * <i><b>Warning:</b> enabling airplane mode on emulator disconnects it from ATMOSPHERE Agent and this emulator can
     * be connected back only after Agent restart. Setting airplane mode for emulators is prohibited</i>
     *
     * @param airplaneMode
     *        - <code>true</code> to enter device in airplane mode, <code>false</code> to exit device from airplane
     *        mode.
     * @return <code>true</code> if the airplane mode setting is successful, <code>false</code> if it fails.
     */
    public boolean setAirplaneMode(boolean airplaneMode) {
        return settingsEntity.setAirplaneMode(airplaneMode);
    }

    /**
     * Enables the screen auto rotation on this device.
     *
     * @return <code>true</code> if the auto rotation setting is successful, and <code>false</code> if it fails
     */
    public boolean enableScreenAutoRotation() {
        return settingsEntity.enableScreenAutoRotation();
    }

    /**
     * Disables the screen auto rotation on this device.
     *
     * @return <code>true</code> if the auto rotation setting is successful, and <code>false</code> if it fails
     */
    public boolean disableScreenAutoRotation() {
        return settingsEntity.disableScreenAutoRotation();
    }

    /**
     * Sets new orientation in space of this device.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @param deviceOrientation
     *        - new {@link DeviceOrientation DeviceOrientation} to be set.
     * @return <code>true</code> if the orientation setting is successful, <code>false</code> if it fails.
     * @deprecated
     */
    @Deprecated
    public boolean setDeviceOrientation(DeviceOrientation deviceOrientation) {
        communicator.sendAction(RoutingAction.SET_ORIENTATION, deviceOrientation);
        // TODO validation maybe?
        return true;
    }

    /**
     * Locks the device.
     *
     * @return <code>true</code> if the lock state setting is successful, <code>false</code> if it fails
     */
    public boolean lock() {
        return setLockState(true);
    }

    /**
     * Unlocks the device.
     *
     * @return <code>true</code> if the lock state setting is successful, <code>false</code> if it fails
     */
    public boolean unlock() {
        return setLockState(false);
    }

    private boolean setLockState(boolean state) {
        if (state) {
            return isLocked() || pressButton(HardwareButton.POWER);
        } else {
            if (!isLocked()) {
                return true;
            }

            try {
                startActivity(ATMOSPHERE_SERVICE_PACKAGE, ATMOSPHERE_UNLOCK_DEVICE_ACTIVITY, false);
            } catch (ActivityStartingException e) {
                return false;
            }

            waitForAwakeState(WAIT_FOR_AWAKE_STATE_INTERVAL, true);
            pressButton(HardwareButton.BACK);

            return !isLocked();
        }
    }

    /**
     * Wait for changing the awake state of the device.
     *
     * @param timeout
     *        - time for waiting to be changed the awake state
     * @param isAwake
     *        - expected awake status <code>true</code> for awake and <code>false</code> for asleep
     * @return <code>true</code> if the state is changed as expected and <code>false</code> otherwise.
     */
    private boolean waitForAwakeState(int timeout, boolean isAwake) {
        for (int i = 0; i < timeout; i += WAIT_FOR_AWAKE_STATE_INTERVAL) {
            try {
                Thread.sleep(WAIT_FOR_AWAKE_STATE_INTERVAL);
                if (isAwake == isAwake()) {
                    return true;
                }
            } catch (InterruptedException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Sets new magnetic field for this device.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @param deviceMagneticField
     *        - new {@link DeviceMagneticField DeviceMagneticField} to be set.
     * @return <code>true</code> if the magnetic field setting is successful, <code>false</code> if it fails.
     */
    public boolean setMagneticField(DeviceMagneticField deviceMagneticField) {
        Object result = communicator.sendAction(RoutingAction.SET_MAGNETIC_FIELD, deviceMagneticField);
        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Sets new proximity for this device. Can only be applied on <b>emulators</b>. You can use proximity constants from
     * the {@link DeviceProximity} class.
     *
     * @param proximity
     *        - the new proximity to be set
     * @return <code>true</code> if the proximity setting was successful, <code>false</code> otherwise
     */
    public boolean setProximity(float proximity) {
        Object result = communicator.sendAction(RoutingAction.SET_PROXIMITY, proximity);
        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Sets the mobile data state of this device.<br>
     * Can only be applied on <b>emulators</b>.
     *
     * @param state
     *        - {@link MobileDataState} to set.
     * @return <code>true</code> if the mobile data state setting is successful, <code>false</code> if it fails.
     */
    public boolean setMobileDataState(MobileDataState state) {
        Object result = communicator.sendAction(RoutingAction.SET_MOBILE_DATA_STATE, state);

        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Sets the environment power-related properties of this device.<br>
     * <i>On real devices, this manipulation only lasts for limited period of time (until the Android BatteryManager
     * updates the battery information).</i>
     *
     * @param properties
     *        - the new power related environment properties to be set.
     * @return <code>true</code> if the environment manipulation is successful, <code>false</code> otherwise.
     */
    public boolean setPowerProperties(PowerProperties properties) {
        Object result = communicator.sendAction(RoutingAction.SET_POWER_PROPERTIES, properties);
        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Sets new screen orientation for this device.<br>
     * Implicitly turns off screen auto rotation.
     *
     * @param screenOrientation
     *        - new {@link ScreenOrientation ScreenOrientation} to be set.
     * @return <code>true</code> if the screen orientation setting is successful, <code>false</code> if it fails.
     */
    public boolean setScreenOrientation(ScreenOrientation screenOrientation) {
        return settingsEntity.setScreenOrientation(screenOrientation);
    }

    /**
     * Enables the WiFi of this device.
     *
     * @return <code>true</code> if the WiFi enabling is successful, <code>false</code> if it fails
     */
    public boolean enableWiFi() {
        Object result = communicator.sendAction(RoutingAction.SET_WIFI_STATE, true);

        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Disables the WiFi of this device.
     *
     * @return <code>true</code> if the WiFi disabling is successful, <code>false</code> if it fails
     */
    public boolean disableWiFi() {
        Object result = communicator.sendAction(RoutingAction.SET_WIFI_STATE, false);

        return result == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Starts an Activity from a package on this device.
     *
     * @param packageName
     *        - package name from which an activity should be started.
     * @param activityName
     *        - activity name to be started. Expects either absolute name or a name starting with dot (.), relative to
     *        the packageName.
     * @return <code>true</code> if the activity start is successful, <code>false</code> if it fails.
     *
     * @throws ActivityStartingException
     *         when the activity can't be started.
     */
    public boolean startActivity(String packageName, String activityName) throws ActivityStartingException {
        return startActivity(packageName, activityName, true);
    }

    /**
     * Starts an Activity from a package on this device.
     *
     * @param packageName
     *        - package name from which an activity should be started.
     * @param activityName
     *        - activity name to be started. Expects either absolute name or a name starting with dot (.), relative to
     *        the packageName.
     * @param unlockDevice
     *        - if <code>true</code>, unlocks the device before starting the activity.
     * @return <code>true</code> if the activity start is successful, <code>false</code> if it fails.
     * @throws ActivityStartingException
     *         when the package or activity is invalid.
     */
    public boolean startActivity(String packageName, String activityName, boolean unlockDevice)
        throws ActivityStartingException {
        if (unlockDevice) {
            setLockState(false);
        }

        IntentBuilder intentBuilder = new IntentBuilder(IntentAction.START_COMPONENT);
        intentBuilder.putComponent(packageName + "/" + activityName);
        String query = intentBuilder.buildIntentCommand();
        String response = (String) communicator.sendAction(RoutingAction.EXECUTE_SHELL_COMMAND, query);

        if (response == null || response.contains("Error: Activity class")) {
            // FIXME TBD should this method return false or should it throw an
            // exception?
            String message = "The passed package or Activity was not found.";
            LOGGER.error(message);
            throw new ActivityStartingException(message);
        }
        return true;
    }

    /**
     * Unlocks the device and starts an application on it.
     *
     * @param packageName
     *        - name of the application's package
     *
     * @return <code>true</code> if the application launch is successful and <code>false</code> otherwise
     */
    public boolean startApplication(String packageName) {
        boolean result = startApplication(packageName, true);
        return result;
    }

    /**
     * Starts an application on the device.
     *
     * @param packageName
     *        - name of the application's package
     *
     * @param shouldUnlockDevice
     *        - if <code>true</code>, unlocks the device before starting the application
     *
     * @return <code>true</code> if the application launch is successful and <code>false</code> otherwise
     */
    public boolean startApplication(String packageName, boolean shouldUnlockDevice) {
        if (shouldUnlockDevice) {
            setLockState(false);
        }

        Boolean response = (Boolean) communicator.sendAction(RoutingAction.START_APP, packageName);

        return response;
    }

    /**
     * Uninstalls an application from the device.
     *
     * @param packageName
     *        - name of the application's package
     *
     * @return <code>true</code> if the application was successfully uninstalled, <code>false</code> otherwise
     */
    public boolean uninstallApplication(String packageName) {
        Object response = communicator.sendAction(RoutingAction.UNINSTALL_APP, packageName);

        return response == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Reinstalls a given application by package name and path.
     *
     * @param packageName
     *        - the package name of the application
     * @param pathToApk
     *        - location of the file to be installed
     * @return true if the reinstall was successful, false otherwise
     */
    public boolean reinstallApplication(String packageName, String pathToApk) {
        boolean uninstallResponse = uninstallApplication(packageName);
        if (!uninstallResponse) {
            return false;
        }

        return installAPK(pathToApk);
    }

    /**
     * Simulates a swipe from a point to another unknown point.
     *
     * @param point
     *        - the starting point.
     * @param swipeDirection
     *        - a direction of the swipe action.
     * @return <code>true</code> if the swipe is successful, <code>false</code> if it fails.
     */
    public boolean swipe(Point point, SwipeDirection swipeDirection) {
        return gestureEntity.swipe(point, swipeDirection);
    }

    /**
     * Executes a simple tap on the screen of this device at a specified location point.
     *
     * @param tapPoint
     *        - {@link Point Point} on the screen to tap on.
     *
     * @return <code>true</code> if tapping screen is successful, <code>false</code> if it fails.
     */
    public boolean tapScreenLocation(Point tapPoint) {
        return gestureEntity.tapScreenLocation(tapPoint);
    }

    /**
     * Executes long press on point on the screen with given coordinates and (default) timeout for the gesture
     * {@value #LONG_PRESS_DEFAULT_TIMEOUT} ms.
     *
     * @param pressPoint
     *        - {@link Point point} on the screen where the long press should be executed.
     * @return - true, if operation is successful, and false otherwise.
     */
    public boolean longPress(Point pressPoint) {
        return longPress(pressPoint, LONG_PRESS_DEFAULT_TIMEOUT);
    }

    /**
     * Executes long press on point on the screen with given coordinates and timeout for the gesture in ms.
     *
     * @param pressPoint
     *        - {@link Point point} on the screen where the long press should be executed.
     * @param timeout
     *        - the time in ms, showing how long should the holding part of the gesture continues.
     * @return - true, if operation is successful, and false otherwise.
     */
    public boolean longPress(Point pressPoint, int timeout) {
        return gestureEntity.longPress(pressPoint, timeout);
    }

    /**
     * Drags and drops from point (Point startPoint) to point (Point endPoint).
     *
     * @param startPoint
     *        - start point of the drag and drop gesture
     * @param endPoint
     *        - end point of the drag and drop gesture
     * @return <code>true</code>, if operation is successful, <code>false</code>otherwise
     */
    public boolean drag(Point startPoint, Point endPoint) {
        return gestureEntity.drag(startPoint, endPoint);
    }

    /**
     * Check if there are running processes on the device with the given package
     *
     * @param packageName
     *        - package of the process that we want to check
     * @return - true, if there are running process and false otherwise
     */

    public boolean isProcessRunning(String packageName) {
        return (boolean) communicator.sendAction(RoutingAction.GET_PROCESS_RUNNING, packageName);
    }

    /**
     * ForceStops all the processes containing the given package. Doesn't work for system processes in the Android OS
     * such as phone, sms, etc.
     *
     * @param packageName
     *        - package of the processes that we want to stop.
     * @return - true, if execution of the command is successful, and false otherwise.
     */
    public boolean forceStopProcess(String packageName) {
        Object response = communicator.sendAction(RoutingAction.FORCE_STOP_PROCESS, packageName);
        closeChromeDriver();
        return response == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Stops a background process by given package. Can not be used on system processes. This method kills only
     * processes that are safe to kill and that will not impact the user experience. Usage of this method on a process
     * that contains service will result in process restart.
     *
     * @param packageName
     *        - contains the package of the process.
     */
    public void stopBackgroundProcess(String packageName) {
        communicator.sendAction(RoutingAction.STOP_BACKGROUND_PROCESS, packageName);
    }

    /**
     * Opens the notification bar on the device.
     *
     * @return true if the opening of the notification bar was successful, false otherwise
     */
    public boolean openNotificationBar() {
        return (boolean) communicator.sendAction(RoutingAction.OPEN_NOTIFICATION_BAR);
    }

    /**
     * Opens the quick settings on the device.
     *
     * @return true if the opening of the quick settings was successful, false otherwise
     */
    public boolean openQuickSettings() {
        return (boolean) communicator.sendAction(RoutingAction.OPEN_QUICK_SETTINGS);
    }

    /**
     * Sets the timeout in the system settings, after which the screen is turned off. On emulators the screen is only
     * dimmed
     *
     * @param screenOffTimeout
     *        - timeout in milliseconds, after which the screen is turned off.
     * @return true if the given screen off timeout is successfully set.
     */
    public boolean setScreenOffTimeout(long screenOffTimeout) {
        return settingsEntity.setScreenOffTimeout(screenOffTimeout);
    }

    /**
     * Gets the timeout from the system settings, after which the screen is turned off.
     *
     * @return timeout in milliseconds, after which the screen is turned off.
     */
    public long getScreenOffTimeout() {
        return settingsEntity.getScreenOffTimeout();
    }

    /**
     * Sets a default keyboard by given ID.
     *
     * @param keyboardID
     *        - a keyboard ID
     * @return true if setting the IME is successful and false otherwise.
     */
    public boolean setDefaultIME(String keyboardID) {
        return (boolean) communicator.sendAction(RoutingAction.SET_DEFAULT_INPUT_METHOD, keyboardID);
    }

    /**
     * Sets the Atmosphere IME keyboard as default. The Atmosphere IME is a small android application that is a simple
     * implementation of input keyboard for Android. It is needed in order to make sure we can execute the tests
     * requiring text input.
     *
     * @return true if setting the IME is successful and false otherwise.
     */
    public boolean setAtmosphereIME() {
        return (boolean) communicator.sendAction(RoutingAction.SET_ATMOSPHERE_IME_AS_DEFAULT);
    }

    /**
     * Gets the {@link DeviceSettingsManager settings manager} of the current device, that allows getting and inserting
     * device settings.
     *
     * @return {@link DeviceSettingsManager} instance for this device
     */
    public DeviceSettingsManager getDeviceSettingsManager() {
        return settingsEntity.getDeviceSettingsManager();
    }

    /**
     * Mocks the location of the device with the one specified in the passed location object.
     *
     * @param mockLocation
     *        - the location to be mocked
     * @return <code>true</code> if the location of the device was successfully mocked, <code>false</code> otherwise
     */
    public boolean mockLocation(GeoLocation mockLocation) {
        return (Boolean) communicator.sendAction(RoutingAction.MOCK_LOCATION, mockLocation);
    }

    /**
     * Disables passing mock location data for the provider with the given name.
     *
     * @param providerName
     *        - the provider whose mocking should be disabled
     */
    public void disableMockLocation(String providerName) {
        communicator.sendAction(RoutingAction.DISABLE_MOCK_LOCATION, providerName);
    }

    /**
     * Dismisses and re-enables the keyguard of the device in order to Lock and Unlock it. The keyguard should be
     * re-enabled for the device's lock to work properly again.
     *
     * @param keyguardStatus
     *        - <code>true</code> if the keyguard should be re-enabled and <code>false</code> to dismiss it.
     */
    public void setKeyguard(boolean keyguardStatus) {
        communicator.sendAction(RoutingAction.SET_KEYGUARD, keyguardStatus);
    }

    /**
     * Gets all task that are currently running on the device, with the most recent being first and older ones after in
     * order.
     *
     * @param maxNum
     *        - maximum number of task that are going to be get from the device
     *
     * @return array of the running tasks id.
     *         <p>
     *         Note: Useful with {@link #bringTaskToFront(int, int) bringTaskToFront} and
     *         {@link #waitForTasksUpdate(int, int, int) waitForTaskUpdate}.
     *         </p>
     *
     * @deprecated Since LOLLIPOP, this method is no longer available. It will still return a small subset of its data:
     *             at least the caller's own tasks, and possibly some other tasks such as home that are known to not be
     *             sensitive.
     */
    @Deprecated
    public int[] getRunningTaskIds(int maxNum) {
        return (int[]) communicator.sendAction(RoutingAction.GET_RUNNING_TASK_IDS, maxNum);
    }

    /**
     * Bring the given task to the foreground of the screen.
     *
     * @param taskId
     *        - the id of the task that is going to be brought to the foreground.
     * @param timeout
     *        - to wait before bringing the task to the foreground.
     * @return <code>true</code> if the task is successfully brought on the foreground and <code>false</code> otherwise.
     */
    public boolean bringTaskToFront(int taskId, int timeout) {
        return (boolean) communicator.sendAction(RoutingAction.BRING_TASK_TO_FRONT, taskId, timeout);
    }

    /**
     * Waits for the given task to be moved to given position in running tasks.
     *
     * @param taskId
     *        - the id of the task.
     * @param position
     *        - the position of the task in which it should be after the update.
     * @param timeout
     *        - to wait for updating the task.
     * @return <code>true</code> if the task is updated and <code>false</code> otherwise.
     *
     * @deprecated Since LOLLIPOP, this method is no longer avaible.
     */
    @Deprecated
    public boolean waitForTasksUpdate(int taskId, int position, int timeout) {
        return (boolean) communicator.sendAction(RoutingAction.WAIT_FOR_TASKS_UPDATE, taskId, position, timeout);
    }

    /**
     * Simulates the given gesture.
     *
     * @param gesture
     *        - the gesture to be executed.
     * @return <code>true</code> if the gesture is executed successfully, <code>false</code> otherwise.
     */
    public boolean playGesture(Gesture gesture) {
        Object response = communicator.sendAction(RoutingAction.PLAY_GESTURE, gesture);
        return response == DeviceCommunicator.VOID_SUCCESS;
    }

    /**
     * Checks if the given image is present on the screen of the device.
     *
     * @param image
     *        - image that will be sought for on the active screen
     * @return <code>true</code> if the image is present on the screen of the device and <code>false</code> otherwise
     * @throws GettingScreenshotFailedException
     *         if getting screenshot from the device failed
     */
    public boolean isImagePresentOnScreen(Image image) throws GettingScreenshotFailedException {
        Image currentScreenImage = getDeviceScreenshotImage();
        return currentScreenImage.containsImage(image);
    }

    /**
     * Gets a screenshot from the device as buffered image.
     *
     * @return BufferedImage that contains the screenshot from the device
     * @throws GettingScreenshotFailedException
     *         if getting screenshot from the device fails
     */
    public Image getDeviceScreenshotImage() throws GettingScreenshotFailedException {
        byte[] imageInByte = getScreenshot();
        InputStream inputStream = new ByteArrayInputStream(imageInByte);

        try {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            return new Image(bufferedImage);
        } catch (IOException e) {
            throw new GettingScreenshotFailedException("Getting screenshot from the device failed.", e);
        }
    }

    /**
     * Starts screen recording with a default orientation(portrait).
     * <p>
     * <b>Note:</b> This method works only for Android 4.4 and above.
     * </p>
     * <p>
     * <b>Note:</b> The maximum recording duration is 30 minutes - if the recording process exceeds the time limit, the
     * recorded files will be lost.
     * </p>
     */
    public void startScreenRecording() {
        startScreenRecording(ClientConstants.DEFAULT_SCREEN_RECORD_TIME_LIMIT, false);
    }

    /**
     * Start screen recording with a certain orientation.
     *
     * @param forceLandscape
     *        - <code>true</code> for portrait, <code>false</code> for landscape screen record orientation
     */
    public void startScreenRecording(boolean forceLandscape) {
        startScreenRecording(ClientConstants.DEFAULT_SCREEN_RECORD_TIME_LIMIT, forceLandscape);
    }

    /**
     * Starts screen recording with given maximum duration. The default screen record orientation is portrait.
     * <p>
     * <b>Note:</b> This method works only for Android 4.4 and above.
     * </p>
     * <p>
     * <b>Note:</b> If the recording process exceeds the time limit, the recorded files will be lost.
     * </p>
     *
     * @param timeLimit
     *        - the maximum recording duration in minutes
     */
    public void startScreenRecording(int timeLimit) {
        startScreenRecording(timeLimit, false);
    }

    /**
     * Starts screen recording with given maximum duration and orientation flag.
     * <p>
     * <b>Note:</b> This method works only for Android 4.4 and above.
     * </p>
     * <p>
     * <b>Note:</b> If the recording process exceeds the time limit, the recorded files will be lost.
     * </p>
     *
     * @param timeLimit
     *        - the maximum recording duration in minutes
     * @param forceLandscape
     *        - <code>true</code> for portrait, <code>false</code> for landscape screen record orientation
     */
    public void startScreenRecording(int timeLimit, boolean forceLandscape) {
        screenRecordUploadDiectoryName = ConfigurationPropertiesLoader.isConfigExists()
                && ConfigurationPropertiesLoader.hasFtpServer()
                        ? ConfigurationPropertiesLoader.getFtpRemoteUplaodDirectory() : "";

        communicator.sendAction(RoutingAction.START_RECORDING, timeLimit, forceLandscape);
        isScreenRecordingStarted = true;
    }

    /**
     * Stops screen recording. If the Agent is connected to an FTP server the video record will be uploaded to a folder
     * with name specified by the client (properties file in the test project working directory).
     */
    public void stopScreenRecording() {
        communicator.sendAction(RoutingAction.STOP_RECORDING, screenRecordUploadDiectoryName);
        isScreenRecordingStarted = false;
    }

    /**
     * Check if the GPS location is enabled on this device.
     *
     * @return <code>true</code> if the GPS location is enabled, <code>false</code> if it's disabled
     */
    public boolean isGpsLocationEnabled() {
        return gpsLocationEntity.isGpsLocationEnabled();
    }

    /**
     * Enables the GPS location on this device.
     *
     * @return <code>true</code> if the GPS location enabling is successful, <code>false</code> if it fails
     */
    public boolean enableGpsLocation() {
        return gpsLocationEntity.enableGpsLocation();
    }

    /**
     * Disables the GPS location on this device.
     *
     * @return <code>true</code> if the GPS location disabling is successful, <code>false</code> if it fails
     */
    public boolean disableGpsLocation() {
        return gpsLocationEntity.disableGpsLocation();
    }

    /**
     * Checks if any audio is currently playing on the device.
     *
     * @return <code>true</code> if an audio is playing, <code>false</code> otherwise
     */
    public Boolean isAudioPlaying() {
        return (boolean) communicator.sendAction(RoutingAction.IS_AUDIO_PLAYING);
    }

    /**
     * Gets the text of the last detected toast message.
     *
     * @return the text of the last toast message or <code>null</code> if such is not detected yet
     */
    public String getLastToast() {
        Object response = communicator.sendAction(RoutingAction.GET_LAST_TOAST);

        if (!(response instanceof String)) {
            return null;
        }

        return (String) response;
    }

    /**
     * Clears the data of a given application.
     *
     * @param packageName
     *        - the package name of the application
     */
    public void clearApplicationData(String packageName) {
        communicator.sendAction(RoutingAction.CLEAR_APP_DATA, packageName);
    }

    /**
     * Gets the current available disk space on the device.
     *
     * @return the available disk space in megabytes
     */
    public Long getAvailableDiskSpace() {
        return (Long) communicator.sendAction(RoutingAction.GET_AVAILABLE_DISK_SPACE);
    }

    /**
     * Sets WiFi connection properties for this device.
     *
     * @param connectionProperties
     *        - {@link com.musala.atmosphere.commons.connectivity.WifiConnectionProperties properties} of the WiFi
     *        connection to be set
     * @return <code>true</code> if WiFi properties are set, <code>false</code> otherwise
     */
    public Boolean setWifiConnectionProperties(WifiConnectionProperties connectionProperties) {
        return (Boolean) communicator.sendAction(RoutingAction.SHAPE_DEVICE, connectionProperties);
    }

    /**
     * Restores WiFi connection properties for this device.
     *
     * @return <code>true</code> if WiFi properties are restored, <code>false</code> otherwise
     */
    public Boolean restoreWifiConnectionProperties() {
        return (Boolean) communicator.sendAction(RoutingAction.UNSHAPE_DEVICE);
    }

    /**
     * Starts local LogCat logging with VERBOSE log level. The LogCat file will be saved in "/logcat" folder in the
     * current project directory.
     */
    public void startLogcat() {
        startLogcat(LOCAL_DIR, LogCatLevel.VERBOSE);
    }

    /**
     * Starts local LogCat logging by log file path and {@link LogCatLevel} levels.
     *
     * @param logFilePath
     *        - path to the log file where device log will be stored
     * @param logLevels
     *        - log levels to be set for filtering
     */
    public void startLogcat(String logFilePath, LogCatLevel... logLevels) {
        String logcatParams = calculateLogLevels(logLevels);
        startLogcat(logFilePath, logcatParams);
    }

    /**
     * Starts local LogCat logging by log file path, {@link LogCatLevel} levels and tag.
     *
     * @param logFilePath
     *        - path to the log file where device log will be stored
     * @param logLevel
     *        - log level to be set for filtering
     * @param tag
     *        - tag for filtering used in combination with the given logLevel
     */
    public void startLogcat(String logFilePath, LogCatLevel logLevel, String tag) {
        String logcatParams = logLevel.getLevelTagFilter(tag) + LogCatLevel.SILENT.getFilterValue();
        startLogcat(logFilePath, " -s " + logcatParams);
    }

    /**
     * Starts local LogCat logging by log file {@link LogCatLevel} level and tag. The LogCat file will be saved in
     * "/logcat" folder in the current project directory.
     *
     * @param logLevel
     *        - log level to be set for filtering
     * @param tag
     *        - tag for filtering used in combination with the given logLevel
     */
    public void startLogcat(LogCatLevel logLevel, String tag) {
        startLogcat(LOCAL_DIR, logLevel, tag);
    }

    /**
     * Starts local LogCat logging by log file {@link LogCatLevel} levels. The LogCat file will be saved in "/logcat"
     * folder in the current project directory.
     *
     * @param logLevels
     *        - log levels to be set for filtering
     */
    public void startLogcat(LogCatLevel... logLevels) {
        startLogcat(LOCAL_DIR, logLevels);
    }

    /**
     * Starts the LogCat buffering and creates a log file that contains test class name and caller test method name.
     *
     * @param logcatFolderPath
     *        - LogCat file path
     * @param logcatParams
     *        - LogCat command parameters
     */
    private void startLogcat(String logcatFolderPath, String logcatParams) {
        if (logcatFolderPath.equals(LOCAL_DIR)) {
            logcatFolderPath = createLogcatOutputFolder(logcatFolderPath);
        }
        logcatFolderPath = addFileSeparatorIfNotExists(logcatFolderPath);
        final String deviceSerialNumber = this.getInformation().getSerialNumber();
        final String command = String.format("adb -s %s logcat %s", deviceSerialNumber, logcatParams);

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        int testMethodTraceLevel = 3;
        String callerMethodName = stackTraceElements[testMethodTraceLevel].getMethodName();
        String callerClassName = stackTraceElements[testMethodTraceLevel].getClassName();
        String filename = logcatFolderPath + callerClassName + "." + callerMethodName;

        filename = composeBaseLogcatFileName(filename + "_");

        Runnable startDeviceLogcatStream = new Runnable() {
            @Override
            public void run() {
                communicator.sendAction(RoutingAction.START_DEVICE_LOGCAT, deviceSerialNumber, command);
            }
        };
        Thread startDeviceLogcatStreamThread = new Thread(startDeviceLogcatStream);
        startDeviceLogcatStreamThread.start();

        getLogcatBuffer(filename);
    }

    /**
     * Prints/Write data from the LogCat buffer and returns the total number of the lines.
     *
     * @param bufferedWriter
     *        - a {@link BufferedWriter}
     * @param buffer
     *        - {@link Pair} of control number and log data
     * @param expectedLineId
     *        - a control ID that verifies that the log data is received in consistent order
     * @return the ID of the next expected line from the buffer.
     * @throws IOException
     *         throw when fails to write the data from the buffer
     */
    private int printBuffer(BufferedWriter bufferedWriter, List<Pair<Integer, String>> buffer, int expectedLineId)
        throws IOException {
        if (buffer.size() > 0) {
            for (Pair<Integer, String> idToLogLine : buffer) {
                if (expectedLineId != idToLogLine.getKey()) {
                    LOGGER.error("Some logcat output is missing.");
                }
                System.out.println(idToLogLine.getValue());
                bufferedWriter.write(idToLogLine.getValue() + System.getProperty("line.separator"));
                expectedLineId++;
            }
        }

        return expectedLineId;
    }

    /**
     * Gets dynamically the new data from the LogCat buffer and prints on the console.
     *
     * @param filename
     *        - the name of the log file
     */
    private void getLogcatBuffer(final String filename) {
        final String serialNumber = this.getInformation().getSerialNumber();

        Runnable getLogcatBuffer = new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                int expectedLineId = 0;
                try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename),
                                                                                               "UTF-8"))) {
                    while (isLogcatEnabled) {
                        List<Pair<Integer, String>> buffer = (List<Pair<Integer, String>>) communicator.sendAction(RoutingAction.GET_LOGCAT_BUFFER,
                                                                                                                   serialNumber);
                        expectedLineId = printBuffer(bufferedWriter, buffer, expectedLineId);
                    }
                } catch (IOException e) {
                    LOGGER.error(String.format("Storing file for device with serial number %s failed.", serialNumber),
                                 e);
                }
            }
        };

        Thread getLogcatBufferThread = new Thread(getLogcatBuffer);
        getLogcatBufferThread.start();
    }

    /**
     * Stops the LogCat buffering and the ADB process on the agent.
     */
    public void stopLogcat() {
        isLogcatEnabled = false;
        String deviceSerialNumber = this.getInformation().getSerialNumber();
        communicator.sendAction(RoutingAction.STOP_LOGCAT, deviceSerialNumber);
    }

    /**
     * Clears the LogCat log from the device.
     */
    public void clearLogcat() {
        communicator.sendAction(RoutingAction.CLEAR_LOGCAT);
    }

    /**
     * Resolves getting the device LogCat for various sets of parameters.
     *
     * @param properies
     *        - {@link LogcatAnnotationProperties} properties for the annotation
     * @return <code>true</code> if device log is stored successfully, <code>false</code> otherwise
     */
    public boolean getDeviceLog(LogcatAnnotationProperties properies) {
        boolean result = false;
        String logcatFolderPath = properies.getLocalOuputPath();
        LogCatLevel[] logcatLevels = properies.getLogCatLevel();
        String tag = properies.getTag();

        if (logcatFolderPath.equals(".")) {
            logcatFolderPath = createLogcatOutputFolder(logcatFolderPath);
        }

        if (tag.isEmpty()) {
            result = this.getDeviceLog(logcatFolderPath, logcatLevels);
        } else if (logcatLevels.length == 1) {
            result = this.getDeviceLog(logcatFolderPath, logcatLevels[0], tag);
        } else {
            LOGGER.error("Failed to get the logcat due incorrect set of annotation parameters.");
        }

        return result;
    }

    /**
     * Stores currently available logs from the device LogCat into a file with the given path. {@link LogCatLevel LogCat
     * levels} are applied as filters, if present. Multiple levels can be used at once for filtering.
     *
     * @param logFilePath
     *        - path to the log file where device log will be stored
     * @param logLevels
     *        - log levels that can be use for filtering, tag can be set to each level
     * @return <code>true</code> if device log is stored successfully, <code>false</code> otherwise
     */
    public boolean getDeviceLog(String logFilePath, LogCatLevel... logLevels) {
        String logcatParams = calculateLogLevels(logLevels);

        return getDeviceLogcat(logFilePath, logcatParams);
    }

    /**
     * Stores currently available logs from the device LogCat into a file with the given path. Retrieved logs are
     * filtered using the {@link LogCatLevel} and the given tag, all the other logs are suppressed.
     *
     * @param logFilePath
     *        - path to the log file where device log will be stored
     * @param logLevel
     *        - log level to be set for filtering
     * @param tag
     *        - tag for filtering used in combination with the given logLevel
     * @return <code>true</code> if device log is stored successfully, <code>false</code> otherwise
     */
    public boolean getDeviceLog(String logFilePath, LogCatLevel logLevel, String tag) {
        return getDeviceLogcat(logFilePath, logLevel.getLevelTagFilter(tag) + LogCatLevel.SILENT.getFilterValue());
    }

    /**
     * Stores into a file currently available logs filtered by tag.
     *
     * @param logFilePath
     *        - path to the log file where device log will be stored
     * @param tag
     *        - tag used for filtering retrieved logs
     * @return <code>true</code> if device log is stored successfully, <code>false</code> otherwise
     */
    public boolean getDeviceLog(String logFilePath, String tag) {
        return getDeviceLogcat(logFilePath, " -s " + tag);
    }

    /**
     * Sends LogCat command for retrieving log information to be executed on the device.
     *
     * @param logFilePath
     *        - path to the log file where device log will be stored
     * @param logFilters
     *        - all the filters that must applied with the command
     * @return <code>true</code> if device log is stored successfully, <code>false</code> otherwise
     */
    private boolean getDeviceLogcat(String logFilePath, String logFilters) {
        byte[] data = (byte[]) communicator.sendAction(RoutingAction.GET_DEVICE_LOGCAT, logFilters);

        return writeLogFile(logFilePath, data);
    }

    /**
     * Stores the given byte sequence into a file with the specified path.
     *
     * @param filePath
     *        - path to the file
     * @param data
     *        - sequence of bytes to be stored
     * @return <code>true</code> if data is stored in the file, <code>false</code> otherwise
     */
    private boolean writeLogFile(String filePath, byte[] data) {
        filePath = addFileSeparatorIfNotExists(filePath);
        filePath = composeBaseLogcatFileName(filePath);
        File localFile = new File(filePath);

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(localFile);
            fileOutputStream.write(data);
            fileOutputStream.close();
        } catch (IOException e) {
            String serialNumber = getInformation().getSerialNumber();
            LOGGER.error(String.format("Storing file for device with serial number %s failed.", serialNumber), e);
            return false;
        }

        return true;
    }

    /**
     * Adds a file separator at the end of the file path if a separator not exists.
     *
     * @param filePath
     *        - file path
     * @return file path
     */
    private String addFileSeparatorIfNotExists(String filePath) {
        if (!filePath.endsWith("\\") && !filePath.endsWith("/")) {
            filePath += File.separator;
        }

        return filePath;
    }

    /**
     * Composes an unique name for the device LogCat file.
     *
     * @param basename
     *        - a base file name
     * @return an unique filename for device LogCat
     */
    private String composeBaseLogcatFileName(String basename) {
        String serialNumber = getInformation().getSerialNumber();
        String model = getInformation().getModel();
        basename += "device_" + model + "_" + serialNumber + ".log";
        basename = basename.replaceAll("\\s+", "_");

        return basename;
    }

    /**
     * Concatenates all log levels
     *
     * @param logLevels
     *        - log levels to be set for filtering
     * @return concatenated log levels
     */
    private String calculateLogLevels(LogCatLevel... logLevels) {
        StringBuilder logFilters = new StringBuilder();

        if (logLevels.length == 0) {
            logFilters.append(LogCatLevel.VERBOSE.getFilterValue());
        }

        for (int i = 0; i < logLevels.length; i++) {
            logFilters.append(logLevels[i].getFilterValue());
        }

        return logFilters.toString();
    }

    private String createLogcatOutputFolder(String logcatFolderPath) {
        logcatFolderPath += File.separator + "logcat";
        File logcatOutputFolder = new File(logcatFolderPath);
        if (!logcatOutputFolder.exists()) {
            logcatOutputFolder.mkdir();
        }

        return logcatFolderPath;
    }

    /**
     * Gets the UIAutomator UI XML dump and saves it in a file.
     *
     * @param pathToXmlFile
     *        - full path to the location at which the XML file should be saved
     * @return <code>true</code> if getting XML operation is successful, <code>false</code> if it fails
     */
    public boolean getUiXml(String pathToXmlFile) {
        String uiHierarchy = (String) communicator.sendAction(RoutingAction.GET_UI_XML_DUMP);

        try (Writer bWritter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathToXmlFile),
                                                                         "UTF-8"))) {
            bWritter.write(uiHierarchy);
        } catch (IOException e) {
            String message = "Saving the xml file failed.";
            LOGGER.error(message, e);
            return false;
        }

        return true;
    }

    /**
     * Closes the instance of the Chrome driver that is currently in use.
     */
    private void closeChromeDriver() {
        communicator.sendAction(RoutingAction.CLOSE_CHROME_DRIVER);
    }

    /**
     * Sets the {@link GpsLocationEntity entity} responsible for executing operations related with changing the GPS
     * location state.
     *
     * @param gpsLocationEntity
     *        - instance of the entity which executes GPS location related operations
     */
    void setGpsLocationEntity(GpsLocationEntity gpsLocationEntity) {
        this.gpsLocationEntity = gpsLocationEntity;
    }

    /**
     * Sets the {@link HardwareButtonEntity entity} responsible for executing operations with {@link HardwareButton
     * hardware buttons}.
     *
     * @param hardwareButtonEntity
     *        - instance of the entity that handles pressing hardware buttons
     */
    void setHardwareButtonEntity(HardwareButtonEntity hardwareButtonEntity) {
        this.hardwareButtonEntity = hardwareButtonEntity;
    }

    /**
     * Sets the {@link GestureEntity entity} responsible for executing gestures.
     *
     * @param gestureEntity
     *        - instance of the entity that handles pressing hardware buttons
     */
    void setGestureEntity(GestureEntity gestureEntity) {
        this.gestureEntity = gestureEntity;
    }

    /**
     * Sets the {@link ImeEntity entity} responsible for operations related with the input method engine.
     *
     * @param imeEntity
     *        - instance of the entity that handles operations related with the input method engine
     */
    void setImeEntity(ImeEntity imeEntity) {
        this.imeEntity = imeEntity;
    }

    /**
     * Sets the {@link ImageEntity entity} responsible for operations related with getting screenshots.
     *
     * @param imageEntity
     *        - instance of the entity that handles operations related with getting screenshots
     */
    void setImageEntity(ImageEntity imageEntity) {
        this.imageEntity = imageEntity;
    }

    /**
     * Sets the {@link DeviceSettingsEntity entity} responsible for retrieving and updating device settings.
     *
     * @param settingsEntity
     *        - instance of the entity that handles changing and receiving information for device settings
     */
    void setSettingsEntity(DeviceSettingsEntity settingsEntity) {
        this.settingsEntity = settingsEntity;
    }

    /**
     * Sets the {@link AccessibilityElementEntity entity} responsible for operations realated with
     * {@link AccessibilityUiElement}.
     *
     * @param elementEntity
     *        - instance of the entity that handles operations related with {@link AccessibilityUiElement}
     */
    void setAccessibilityElementEntity(AccessibilityElementEntity elementEntity) {
        this.elementEntity = elementEntity;
    }
}
