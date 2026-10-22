package com.spinny.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CarDetailsPage {
    private final WebDriver driver;
    public CarDetailsPage(WebDriver driver) { this.driver = driver; }

    public void waitUntilLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(25))
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-componentname='ProductDesktop'] h1[data-base-component='heading']")));
    }

    public String title() {
        return driver.findElements(By.cssSelector("[data-componentname='ProductDesktop'] h1[data-base-component='heading']"))
                .stream().filter(e -> e.isDisplayed()).map(e -> e.getText().trim()).filter(s -> !s.isBlank()).findFirst().orElse("");
    }

    public String summaryText() {
        return driver.findElements(By.cssSelector("[data-category='overview']"))
                .stream().filter(e -> e.isDisplayed()).map(e -> e.getText().replaceAll("\\s+", " ").trim())
                .filter(s -> !s.isBlank()).findFirst().map(s -> s.substring(0, Math.min(s.length(), 1200))).orElse("");
    }
}
