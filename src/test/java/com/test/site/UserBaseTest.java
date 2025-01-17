package com.test.site;

import com.appium.manager.AppiumDriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;

import java.time.Duration;

public class UserBaseTest {
    public AppiumDriver driver;

    @BeforeTest
    public void setUp() {
        System.out.println("In Before Method");
    }

    @AfterTest
    public void tearDown() {
        System.out.println("After Method");
    }

    public AppiumDriver getDriver() {
        driver = AppiumDriverManager.getDriver();
        return driver;
    }

    public WebElement login(String locator) {
        return waitForElement(locator);
    }

    public WebElement waitForElement(String locator) {
        return new WebDriverWait(getDriver(), Duration.ofSeconds(30)).until(ExpectedConditions
                .elementToBeClickable(AppiumBy.accessibilityId(locator)));
    }
}
