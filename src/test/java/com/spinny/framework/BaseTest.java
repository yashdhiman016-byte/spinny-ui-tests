package com.spinny.framework;

import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/** Shared TestNG fixture: each test gets a browser and always attempts cleanup. */
public abstract class BaseTest {
    protected WebDriver driver;

    public WebDriver getDriver() { return driver; }

    @BeforeMethod
    public void startBrowser() {
        // TestNG calls this before each test method, keeping browser state isolated.
        driver = DriverFactory.create();
    }

    @AfterMethod(alwaysRun = true)
    public void stopBrowser(ITestResult result) {
        if (driver != null) {
            // Attach evidence before closing the browser, including for failed tests.
            String screenshotName = result.getMethod().getMethodName() + "-"
                    + (result.isSuccess() ? "PASS" : result.getStatus() == ITestResult.SKIP ? "SKIP" : "FAIL") + ".png";
            try {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment(screenshotName, "image/png", new java.io.ByteArrayInputStream(screenshot), ".png");
                ScenarioReportData.put("Screenshot evidence", screenshotName);
            } catch (RuntimeException captureError) {
                ScenarioReportData.put("Screenshot evidence", "Unavailable: " + captureError.getMessage());
                System.err.println("Could not capture test screenshot: " + captureError.getMessage());
            } finally {
                driver.quit();
                driver = null;
            }
        }
    }
}
