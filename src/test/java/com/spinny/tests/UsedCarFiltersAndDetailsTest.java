package com.spinny.tests;

import com.spinny.framework.BaseTest;
import com.spinny.framework.ScenarioReportData;
import com.spinny.pages.CarDetailsPage;
import com.spinny.pages.UsedCarsListingPage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/** Runs budget, make/model search, and year filters independently, then opens a result. */
public class UsedCarFiltersAndDetailsTest extends BaseTest {
    @DataProvider(name = "filterCases")
    public Object[][] filterCases() {
        // TestNG runs each row as a separate test invocation with its own browser lifecycle.
        return new Object[][] {
                {"budget", "₹16L–₹40L", 16_00_000, 40_00_000},
                {"budget", "₹5L–₹14L", 5_00_000, 14_00_000},
                {"budget", "₹30L–₹50L", 30_00_000, 47_00_000},
                {"brand", "Maruti Suzuki", 0, 0},
                {"model", "Swift", 0, 0},
                {"brand", "Hyundai", 0, 0},
                {"model", "Creta", 0, 0},
                {"year", "2022", 2022, 0},
                {"year", "2023", 2023, 0},
                {"year", "2020", 2020, 0}
        };
    }

    @Test(dataProvider = "filterCases")
    public void applySingleFilterAndPrintCarDetails(String filterType, String value, int first, int second) {
        // Keep the listing URL configurable so the same test can target another city.
        driver.get(System.getProperty("listing.url", "https://www.spinny.com/used-cars-in-delhi-ncr/s/"));
        UsedCarsListingPage listing = new UsedCarsListingPage(driver);
        listing.waitUntilLoaded();
        ScenarioReportData.put("Scenario", filterType + " filter: " + value);
        ScenarioReportData.put("Filter applied", "Requested " + filterType + " = " + value);

        String applied;
        // Dispatch to the matching Page Object action; the test focuses on outcomes.
        if (filterType.equals("budget")) {
            String selectedRange = listing.setBudgetRange(first, second);
            applied = "Budget requested ₹" + String.format("%,d", first) + "–₹" + String.format("%,d", second)
                    + "; selected slider range " + selectedRange;
        } else if (filterType.equals("brand")) {
            listing.selectMake(value, value);
            applied = "Brand " + value;
        } else if (filterType.equals("model")) {
            listing.searchFor(value);
            listing.selectModel(value);
            applied = "Model " + value;
        } else {
            int selectedYear = listing.selectYearAtLeast(first);
            applied = "Year " + selectedYear + " & above" + (selectedYear == first ? "" : " (closest available option for " + first + ")");
        }

        List<String> titles = listing.waitForCars();
        // Validate that the filter produced usable listings before opening one.
        Assert.assertFalse(titles.isEmpty(), "Expected cars after applying " + applied);
        String firstPrice = listing.firstCarPrice();
        ScenarioReportData.put("Scenario", filterType + " filter: " + value);
        ScenarioReportData.put("Filter applied", applied);
        ScenarioReportData.put("Filter/listing details shown", "Cars shown: " + titles.size() + "; first results: "
                + String.join(" | ", titles.stream().limit(5).toList()) + "; first price: " + firstPrice);
        System.out.println("\n=== Filter case: " + applied + " ===");
        System.out.println("Cars shown: " + titles.size());
        titles.stream().limit(5).forEach(title -> System.out.println("Car: " + title));
        Assert.assertFalse(firstPrice.isBlank(), "First result should show a price for " + applied);

        String detailUrl = listing.openFirstCar();
        // Verify the end-to-end journey from filtered listing to a car detail page.
        CarDetailsPage details = new CarDetailsPage(driver);
        details.waitUntilLoaded();
        String detailTitle = details.title();
        String detailText = details.summaryText();
        Assert.assertFalse(detailTitle.isBlank(), "Car detail page should show a vehicle title");
        Assert.assertTrue(detailUrl.contains("/buy-used-cars/"), "Expected PDP URL, got: " + detailUrl);
        System.out.println("Opened car: " + detailTitle);
        System.out.println("Detail URL: " + detailUrl);
        System.out.println("Car details: " + detailText);
        ScenarioReportData.put("Opened car", detailTitle);
        ScenarioReportData.put("Detail/listing URL", detailUrl);
        ScenarioReportData.put("Filter/listing details shown", "Cars shown: " + titles.size() + "; first results: "
                + String.join(" | ", titles.stream().limit(5).toList()) + "; first price: " + firstPrice
                + "; detail summary: " + detailText);
    }
}
