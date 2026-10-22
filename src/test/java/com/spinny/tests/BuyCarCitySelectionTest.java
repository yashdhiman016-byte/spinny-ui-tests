package com.spinny.tests;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.spinny.framework.BaseTest;
import com.spinny.framework.ScenarioReportData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class BuyCarCitySelectionTest extends BaseTest {
    private static final String HOME_URL = "https://www.spinny.com/home/";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);

    @DataProvider(name = "cities")
    public Object[][] cities() {
        String selectedCity = System.getProperty("city");
        if (selectedCity != null && !selectedCity.isBlank()) {
            return new Object[][] {{selectedCity}};
        }
        return new Object[][] {
            {"Delhi NCR"}, {"Bangalore"}, {"Hyderabad"}, {"Mumbai"}, {"Pune"},
            {"Delhi"}, {"Gurgaon"}, {"Noida"}, {"Ahmedabad"}, {"Chennai"},
            {"Kolkata"}, {"Lucknow"}, {"Jaipur"}, {"Chandigarh"}
        };
    }

    @Test(dataProvider = "cities")
    public void buyCarOpensListingsForCity(String city) {
            WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT);
            ScenarioReportData.put("Scenario", "Buy car city selection: " + city);
            ScenarioReportData.put("Selected city", city);
            driver.get(HOME_URL);

            By buyCar = By.cssSelector("a[data-componentname='BuyAndSell'][data-label='buy_car_cta']");
            wait.until(ExpectedConditions.elementToBeClickable(buyCar)).click();

            By cityOption = By.xpath(
                "//*[normalize-space(.)=" + xpathLiteral(city) + " and not(*)]"
            );
            WebElement option = wait.until(d -> d.findElements(cityOption).stream()
                    .filter(WebElement::isDisplayed)
                    .findFirst()
                    .orElse(null));
            Assert.assertTrue(option.isDisplayed(), "City option should be visible: " + city);
            WebElement cityCard = option.findElement(By.xpath(
                    "ancestor::div[normalize-space(.)=" + xpathLiteral(city) + "][1]"));
            WebElement cityRipple = cityCard.findElement(By.xpath("../div[contains(@class, 'Ripple__container')]"));
            String selectorUrl = driver.getCurrentUrl();
            try {
                cityRipple.click();
            } catch (ElementClickInterceptedException intercepted) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cityRipple);
            }

            try {
                wait.until(d -> !d.getCurrentUrl().equals(selectorUrl)
                        && d.getCurrentUrl().contains("used-cars"));
            } catch (org.openqa.selenium.TimeoutException timeout) {
                System.err.println("City selection did not navigate. URL: " + driver.getCurrentUrl());
                System.err.println("Visible page text: " + driver.findElement(By.tagName("body")).getText());
                throw timeout;
            }
            By listingHeading = By.cssSelector("h1[data-base-component='heading']");
            wait.until(ExpectedConditions.textToBePresentInElementLocated(listingHeading, city));
            String headingText = driver.findElement(listingHeading).getText();
            Assert.assertTrue(headingText.contains(city),
                    "Listings heading should contain selected city '" + city
                            + "', but heading was: " + headingText);

            By carCards = By.cssSelector("div[data-componentname='CarListingDesktop']");
            By firstCarTitle = By.cssSelector(
                    "div[data-componentname='CarListingDesktop'] h3[data-base-component='heading']");
            wait.until(ExpectedConditions.visibilityOfElementLocated(firstCarTitle));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(carCards));
            java.util.List<WebElement> listings = driver.findElements(carCards);

            System.out.println("\n=== Spinny listings for " + city + " ===");
            System.out.println("Listing summary: " + headingText);
            System.out.println("Listing cards loaded: " + listings.size());
            System.out.println("First " + Math.min(3, listings.size()) + " cars:");
            List<String> displayedCars = new ArrayList<>();
            for (int index = 0; index < Math.min(5, listings.size()); index++) {
                WebElement listing = listings.get(index);
                String title = listing.findElement(By.cssSelector("h3[data-base-component='heading']"))
                        .getText().trim();
                String price = listing.findElements(By.cssSelector("span[data-base-component='Pricing']"))
                        .stream().map(WebElement::getText).filter(text -> !text.isBlank())
                        .findFirst().orElse("Price unavailable").replaceAll("\\s+", " ").trim();
                String variantAndEmi = listing.findElements(By.cssSelector(
                                "[data-componentname='ListingModelEmiDetail']"))
                        .stream().map(WebElement::getText).filter(text -> !text.isBlank())
                        .findFirst().orElse("Details unavailable").replaceAll("\\s+", " ").trim();
                String carInfo = (index + 1) + ". " + title + " | " + price + " | " + variantAndEmi;
                displayedCars.add(carInfo);
                System.out.println(carInfo);
            }
            String listingUrl = driver.getCurrentUrl();
            ScenarioReportData.put("City listing data shown", headingText + "; loaded listing cards: " + listings.size()
                    + "; first cars: " + String.join(" || ", displayedCars));
            ScenarioReportData.put("Detail/listing URL", listingUrl);
            System.out.println("Listings page: " + listingUrl);

            Assert.assertTrue(listingUrl.contains("used-cars"),
                    "Expected to reach used car listings after selecting " + city
                            + ", but URL was " + listingUrl);
    }

    /** Builds a safe XPath string literal, including values containing apostrophes. */
    private static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        String[] parts = value.split("'", -1);
        return "concat('" + String.join("', \"'\", '", parts) + "')";
    }
}
