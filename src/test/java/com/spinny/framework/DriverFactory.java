package com.spinny.framework;

import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/** Central browser setup; tune with -Dheadless=true and -Dbrowser=chrome. */
public final class DriverFactory {
    private DriverFactory() {}

    public static WebDriver create() {
        String browser = System.getProperty("browser", "chrome");
        if (!browser.equalsIgnoreCase("chrome")) {
            throw new IllegalArgumentException("Unsupported browser: " + browser + " (currently chrome)");
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1440,1100", "--disable-notifications");
        String chromeBinary = System.getenv("CHROME_BIN");
        if (chromeBinary != null && !chromeBinary.isBlank()) {
            options.setBinary(chromeBinary);
        }
        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new", "--disable-dev-shm-usage");
            if ("true".equalsIgnoreCase(System.getenv("CI"))) {
                options.addArguments("--no-sandbox");
            }
        }
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        return driver;
    }
}
