package com.appium.utils;

import static java.util.stream.Collectors.toList;

import com.appium.entities.MobilePlatform;
import com.appium.ios.IOSDeviceConfiguration;
import com.appium.manager.AppiumDeviceManager;
import com.appium.manager.AppiumManagerFactory;
import com.appium.manager.ArtifactsUploader;
import com.appium.manager.HostArtifact;
import com.appium.manager.IAppiumManager;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import org.json.JSONObject;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.util.ResourceUtils;

import java.nio.file.FileSystems;
import java.nio.file.Path;


/**
 * Created by saikrisv on 20/05/17.
 */
public class DesiredCapabilityBuilder {

    private AvailablePorts availablePorts;

    public static ThreadLocal<DesiredCapabilities> desiredCapabilitiesThreadLocal
            = new ThreadLocal<>();

    public DesiredCapabilityBuilder() {
        availablePorts = new AvailablePorts();
    }

    public static DesiredCapabilities getDesiredCapability() {
        return desiredCapabilitiesThreadLocal.get();
    }

    public void buildDesiredCapability(String platform,
                                       String jsonPath) throws Exception {
        int port = AppiumDeviceManager.getAppiumDevice().getPort();

        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        JSONObject platFormCapabilities = new JsonParser(jsonPath).getObjectFromJSON()
                .getJSONObject(platform);

        platFormCapabilities.keySet().forEach(key -> {
            if ("browserName".equals(key) && "chrome".equals(platFormCapabilities.getString(key))) {
                try {
                    AppiumDeviceManager.getAppiumDevice().setChromeDriverPort(
                            availablePorts.getAvailablePort(AppiumDeviceManager
                                    .getAppiumDevice().getHostName()));
                    desiredCapabilities.setCapability("chromeDriverPort",
                            AppiumDeviceManager.getAppiumDevice().getChromeDriverPort());
                } catch (Exception e) {
                    new RuntimeException("Unable to allocate chromedriver with unique port");
                }
            }
            String appCapability = "app";
            if (appCapability.equals(key)) {
                Object values = platFormCapabilities.get(appCapability);
                HostArtifact hostArtifact = null;
                hostArtifact = ArtifactsUploader.getInstance()
                        .getHostArtifacts().stream().filter(s ->
                                s.getHost()
                                        .equalsIgnoreCase(AppiumDeviceManager
                                                .getAppiumDevice()
                                                .getHostName()))
                        .collect(toList()).parallelStream()
                        .findFirst().get();
                String appPath = "";
                if (values instanceof JSONObject) {
                    int length = AppiumDeviceManager.getAppiumDevice()
                            .getDevice().getUdid().length();
                    if (length
                            == IOSDeviceConfiguration.SIM_UDID_LENGTH) {
                        appPath = hostArtifact.getArtifactPath("APP");
                    } else if (length
                            == IOSDeviceConfiguration.IOS_UDID_LENGTH) {
                        appPath = hostArtifact.getArtifactPath("IPA");
                    }
                } else {
                    appPath = hostArtifact.getArtifactPath("APK");
                }
                Path path = FileSystems.getDefault().getPath(appPath);
                if (ResourceUtils.isUrl(appPath)) {
                    desiredCapabilities.setCapability(appCapability, appPath);
                } else if (!path.getParent().isAbsolute()) {
                    desiredCapabilities.setCapability(appCapability, path.normalize()
                            .toAbsolutePath().toString());
                } else {
                    desiredCapabilities.setCapability(appCapability, path.toString());
                }
            } else {
                desiredCapabilities.setCapability(key, platFormCapabilities.get(key));
            }
        });

        if (AppiumDeviceManager.getMobilePlatform().equals(MobilePlatform.ANDROID)) {
            desiredCapabilities.setCapability(AndroidMobileCapabilityType.SYSTEM_PORT,
                    port);
            appPackage(desiredCapabilities);
        } else if (AppiumDeviceManager.getMobilePlatform().equals(MobilePlatform.IOS)) {
            String version = AppiumDeviceManager.getAppiumDevice().getDevice().getOsVersion();
            appPackageBundle(desiredCapabilities);
            //Check if simulator.json exists and add the deviceName and OS
            if (AppiumDeviceManager.getAppiumDevice().getDevice().getUdid().length()
                    == IOSDeviceConfiguration.SIM_UDID_LENGTH) {

                AppiumDevice deviceProperty = AppiumDeviceManager.getAppiumDevice();
                desiredCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME,
                        deviceProperty.getDevice().getName());
                desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION,
                        deviceProperty.getDevice().getOsVersion());
                desiredCapabilities.setCapability("webkitDebugProxyPort",
                        new AvailablePorts().getAvailablePort(AppiumDeviceManager
                                .getAppiumDevice().getHostName()));
            } else {
                IAppiumManager appiumManager = AppiumManagerFactory
                        .getAppiumManager(AppiumDeviceManager.getAppiumDevice().getHostName());
                desiredCapabilities.setCapability("webkitDebugProxyPort",
                        new AvailablePorts().getAvailablePort(AppiumDeviceManager
                                .getAppiumDevice().getHostName()));
            }

            desiredCapabilities.setCapability(IOSMobileCapabilityType
                    .WDA_LOCAL_PORT, port);
            desiredCapabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME,
                    AutomationName.IOS_XCUI_TEST);
            desiredCapabilities.setCapability(MobileCapabilityType.UDID,
                    AppiumDeviceManager.getAppiumDevice().getDevice().getUdid());
        }
        desiredCapabilities.setCapability(MobileCapabilityType.UDID,
                AppiumDeviceManager.getAppiumDevice().getDevice().getUdid());
        desiredCapabilitiesThreadLocal.set(desiredCapabilities);
    }

    public void appPackage(DesiredCapabilities desiredCapabilities) {
        if (System.getenv("APP_PACKAGE") != null) {
            desiredCapabilities.setCapability(AndroidMobileCapabilityType.APP_PACKAGE,
                    System.getenv("APP_PACKAGE"));
        }
    }

    private void appPackageBundle(DesiredCapabilities iOSCapabilities) {
        if (System.getenv("APP_PACKAGE") != null) {
            iOSCapabilities
                    .setCapability(IOSMobileCapabilityType.BUNDLE_ID,
                            System.getenv("APP_PACKAGE"));
        }
    }
}
