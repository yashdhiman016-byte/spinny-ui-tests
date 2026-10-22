package com.spinny.pages;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Page object for the used-car PLP. Keep site selectors isolated here for easy maintenance. */
public class UsedCarsListingPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final By filters = By.cssSelector("[data-id='filter-list']");
    private final By cards = By.cssSelector("[data-componentname='CarListingDesktop']");
    private final By filterChips = By.cssSelector("[data-componentname='AppliedFilters']");

    public UsedCarsListingPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    public void waitUntilLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(filters));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-id='landing-plp-container']")));
    }

    /** Configure range handles by keyboard steps from their endpoints (min from low, max from high). */
    public void adjustBudget(int minimumSteps, int maximumSteps) {
        List<WebElement> handles = wait.until(d -> {
            List<WebElement> found = d.findElements(By.cssSelector("[data-componentname='BudgetDesktop'] input[type='range']"));
            return found.size() >= 2 ? found : null;
        });
        moveHandle(handles.get(0), Keys.HOME, Keys.ARROW_RIGHT, minimumSteps);
        moveHandle(handles.get(1), Keys.END, Keys.ARROW_LEFT, maximumSteps);
    }

    private void moveHandle(WebElement handle, Keys endpoint, Keys direction, int steps) {
        // Both native range inputs occupy the same track; the upper handle can intercept
        // pointer clicks on the lower one. sendKeys focuses the selected input directly.
        handle.sendKeys(endpoint);
        for (int i = 0; i < Math.max(0, steps); i++) handle.sendKeys(direction);
        handle.sendKeys(Keys.TAB);
        wait.until(d -> !handle.getAttribute("value").isBlank());
    }

    public void selectMake(String make, String searchText) {
        searchFor(searchText == null || searchText.isBlank() ? make : searchText);
        String id = "make-" + slug(make);
        By rowBy = By.cssSelector("[data-id='" + id + "']");
        WebElement row = wait.until(ExpectedConditions.elementToBeClickable(rowBy));
        if (!isChecked(row)) row.findElement(By.cssSelector("[data-id='" + id + "-box']")).click();
    }

    public void searchFor(String name) {
        WebElement search = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-id='search-model'] input#location")));
        search.clear();
        search.sendKeys(name);
    }

    public void selectModel(String model) {
        String id = "model-" + slug(model);
        By rowBy = By.cssSelector("[data-id='" + id + "']");
        // Model rows live in a collapsed list below their make. Expand that make
        // when the searched model is present in the DOM but not visible yet.
        WebElement hiddenModel = wait.until(d -> d.findElements(rowBy).stream().findFirst().orElse(null));
        if (!hiddenModel.isDisplayed()) {
            WebElement makeRow = hiddenModel.findElement(By.xpath("ancestor::ul[1]/preceding-sibling::div[1]"));
            WebElement arrow = makeRow.findElement(By.xpath("./div[last()]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click()", arrow);
        }
        WebElement row = wait.until(d -> {
            List<WebElement> byId = d.findElements(rowBy);
            for (WebElement candidate : byId) {
                if (candidate.isDisplayed()) return candidate;
            }
            // The live listing uses text based model rows; data-id values can differ
            // from the make/model slugs and change with the site's filter redesign.
            String exactText = "//*[normalize-space(text())=" + xpathLiteral(model) + "]";
            for (WebElement candidate : d.findElements(By.xpath(exactText))) {
                if (candidate.isDisplayed() && !candidate.findElements(By.xpath("ancestor::*[@data-id][1]")).isEmpty()) {
                    return candidate.findElement(By.xpath("ancestor::*[@data-id][1]"));
                }
            }
            return null;
        });
        if (!isChecked(row)) row.click();
    }

    /** Move range handles to the closest supported currency values and return the values shown. */
    public String setBudgetRange(int minimumRupees, int maximumRupees) {
        List<WebElement> handles = wait.until(d -> {
            List<WebElement> found = d.findElements(By.cssSelector("[data-componentname='BudgetDesktop'] input[type='range']"));
            return found.size() >= 2 ? found : null;
        });
        moveToBudgetValue(true, minimumRupees);
        moveToBudgetValue(false, maximumRupees);
        // Let the listing's debounced filter request finish before reading cards.
        wait.until(d -> {
            String min = budgetValue(true);
            String max = budgetValue(false);
            return !min.isBlank() && !max.isBlank();
        });
        try { Thread.sleep(900); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for budget results", e);
        }
        String min = driver.findElement(By.cssSelector("[data-id='budget-value-min']")).getText().replaceAll("[^0-9]", "");
        String max = driver.findElement(By.cssSelector("[data-id='budget-value-max']")).getText().replaceAll("[^0-9]", "");
        return "₹" + min + "–₹" + max;
    }

    private void moveToBudgetValue(boolean minimumHandle, int requested) {
        By inputBy = By.cssSelector("[data-componentname='BudgetDesktop'] input[type='range']:" +
                (minimumHandle ? "first-of-type" : "last-of-type"));
        Keys endpoint = minimumHandle ? Keys.HOME : Keys.END;
        Keys direction = minimumHandle ? Keys.ARROW_RIGHT : Keys.ARROW_LEFT;
        handle(inputBy).sendKeys(endpoint);
        long current = parseBudget(budgetValue(minimumHandle));
        long previous = current;
        for (int step = 0; step < 100; step++) {
            if ((minimumHandle && current >= requested) || (!minimumHandle && current <= requested)) break;
            handle(inputBy).sendKeys(direction);
            final long old = current;
            wait.until(d -> parseBudget(budgetValue(minimumHandle)) != old);
            previous = current;
            current = parseBudget(budgetValue(minimumHandle));
            // Stop on the nearest available slider point instead of overshooting by a step.
            if ((minimumHandle && current >= requested) || (!minimumHandle && current <= requested)) {
                if (Math.abs(previous - requested) <= Math.abs(current - requested)) {
                    handle(inputBy).sendKeys(minimumHandle ? Keys.ARROW_LEFT : Keys.ARROW_RIGHT);
                    final long selectedValue = previous;
                    wait.until(d -> parseBudget(budgetValue(minimumHandle)) == selectedValue);
                }
                break;
            }
        }
        handle(inputBy).sendKeys(Keys.TAB);
    }

    private WebElement handle(By locator) {
        return wait.until(d -> d.findElement(locator));
    }

    private String budgetValue(boolean minimumHandle) {
        List<WebElement> values = driver.findElements(By.cssSelector(minimumHandle
                ? "[data-id='budget-value-min']" : "[data-id='budget-value-max']"));
        return values.isEmpty() ? "" : values.get(0).getText().replaceAll("[^0-9]", "");
    }

    private long parseBudget(String label) {
        return label.isBlank() ? -1 : Long.parseLong(label);
    }

    public int selectYearAtLeast(int year) {
        WebElement option = wait.until(d -> d.findElements(By.cssSelector("[data-componentname='FilterRadio'][data-id^='radio-']"))
                .stream().filter(WebElement::isDisplayed)
                .filter(e -> e.getText().matches("(?s).*20\\d{2}\\s*&\\s*above.*"))
                .filter(e -> Integer.parseInt(e.getText().replaceAll("(?s).*?(20\\d{2}).*", "$1")) >= year)
                .min(java.util.Comparator.comparingInt(e -> Integer.parseInt(e.getText().replaceAll("(?s).*?(20\\d{2}).*", "$1"))))
                .orElse(null));
        if (option == null) throw new IllegalStateException("No year filter option available for threshold " + year);
        int selectedYear = Integer.parseInt(option.getText().replaceAll("(?s).*?(20\\d{2}).*", "$1"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", option);
        wait.until(ExpectedConditions.visibilityOf(option));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
        return selectedYear;
    }

    public List<String> waitForCars() { return visibleCarTitles(); }

    public List<WebElement> visibleCars() {
        return wait.until(d -> {
            try {
                List<WebElement> found = d.findElements(cards).stream().filter(WebElement::isDisplayed)
                        .filter(card -> !card.findElements(By.cssSelector("h3[data-base-component='heading']")).isEmpty())
                        .toList();
                return found.isEmpty() ? null : found;
            } catch (StaleElementReferenceException rerendering) {
                return null;
            }
        });
    }

    public String firstCarTitle() {
        return visibleCars().get(0).findElement(By.cssSelector("h3[data-base-component='heading']")).getText().trim();
    }

    public List<String> visibleCarTitles() {
        return visibleCars().stream().map(card -> card.findElement(By.cssSelector("h3[data-base-component='heading']")))
                .map(WebElement::getText).map(String::trim).toList();
    }

    public String waitForCarMatching(String make, int minimumYear) {
        return wait.until(d -> {
            try {
                return d.findElements(cards).stream().filter(WebElement::isDisplayed)
                        .flatMap(card -> card.findElements(By.cssSelector("h3[data-base-component='heading']")).stream())
                        .map(WebElement::getText).map(String::trim)
                        .filter(title -> title.toLowerCase().contains(make.toLowerCase()))
                        .filter(title -> {
                            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("20\\d{2}").matcher(title);
                            return matcher.find() && Integer.parseInt(matcher.group()) >= minimumYear;
                        }).findFirst().orElse(null);
            } catch (StaleElementReferenceException rerendering) {
                return null;
            }
        });
    }

    public String firstCarPrice() {
        return visibleCars().get(0).findElements(By.cssSelector("[data-base-component='Pricing']"))
                .stream().map(WebElement::getText).filter(s -> !s.isBlank()).findFirst().orElse("").trim();
    }

    public boolean hasAppliedFilter(String text) {
        return driver.findElements(filterChips).stream().filter(WebElement::isDisplayed)
                .anyMatch(e -> e.getText().toLowerCase().contains(text.toLowerCase()));
    }

    public String openFirstCar() {
        WebElement card = visibleCars().get(0);
        String prior = driver.getCurrentUrl();
        List<WebElement> overlays = card.findElements(By.cssSelector(".ds-absolute.ds-overflow-hidden.ds-interactive[role='none']"));
        List<WebElement> links = card.findElements(By.cssSelector("a[href*='/buy-used-cars/']"));
        if (!links.isEmpty()) ((JavascriptExecutor) driver).executeScript("arguments[0].click();", links.get(0));
        else if (!overlays.isEmpty()) overlays.get(0).click();
        else card.click();
        wait.until(d -> !d.getCurrentUrl().equals(prior));
        return driver.getCurrentUrl();
    }

    private static String slug(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private static boolean isChecked(WebElement row) {
        return !row.findElements(By.cssSelector("[data-componentname='FilterCheckIcon'] svg")).isEmpty();
    }

    private static String xpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        return "concat('" + String.join("', \"'\", '", value.split("'", -1)) + "')";
    }
}
