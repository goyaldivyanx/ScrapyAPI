package org.divyansh;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OneMgScraper {
    private static final Logger logger = LoggerFactory.getLogger(OneMgScraper.class);

    // City to pincode mapping
    private static final Map<String, String> CITY_PINCODES = new HashMap<>();

    static {
        CITY_PINCODES.put("Delhi", "110001");
        CITY_PINCODES.put("Mumbai", "400001");
        CITY_PINCODES.put("Bangalore", "560001");
        CITY_PINCODES.put("Chennai", "600001");
        // Add more cities if needed
    }

    public static Map<String, String> get1mgBestDeal(String medicine, String city) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        Map<String, String> result = new HashMap<>();

        try {
            driver.get("https://www.1mg.com");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            String pincode = CITY_PINCODES.getOrDefault(city, "110001");
            try {
                WebElement locationButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("div[class*='location']")));
                locationButton.click();

                WebElement pincodeInput = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("input[placeholder*='Pincode'], input[placeholder*='PINCODE']")));
                pincodeInput.sendKeys(pincode);
                pincodeInput.sendKeys(Keys.ENTER);

                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("span[class*='style__city']")));
                logger.info("Set location to {} (pincode: {})", city, pincode);
            } catch (Exception e) {
                logger.warn("Failed to set location: {}. Using default.", e.getMessage());
            }

            try {
                WebElement popup = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button[class*='style__close'], div[class*='style__close']")));
                popup.click();
                logger.info("Closed popup");
            } catch (Exception ignored) {
                logger.info("No popup found");
            }

            WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[placeholder*='Search for Medicines']")));
            searchBox.sendKeys(medicine);
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("div[class*='style__suggestion']")));
            } catch (Exception e) {
                logger.warn("Autocomplete did not show up, hitting ENTER anyway.");
            }
            searchBox.sendKeys(Keys.ENTER);

            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div[class*='style__horizontal-card']")));
            List<WebElement> cards = driver.findElements(By.cssSelector("div[class*='style__horizontal-card']"));
            logger.info("Found {} cards", cards.size());

            double bestPrice = Double.MAX_VALUE;
            WebElement bestCard = null;

            for (WebElement card : cards) {
                try {
                    String cardText = card.getText();
                    double price = Double.MAX_VALUE;
                    try {
                        WebElement priceEl = card.findElement(By.cssSelector("div[class*='style__price']"));
                        String priceText = priceEl.getText().replaceAll("[^0-9.]", "");
                        price = Double.parseDouble(priceText);
                    } catch (Exception e) {
                        Matcher matcher = Pattern.compile("\u20B9([0-9.]+)").matcher(cardText);
                        if (matcher.find()) price = Double.parseDouble(matcher.group(1));
                    }

                    if (price < bestPrice) {
                        bestPrice = price;
                        bestCard = card;
                    }
                } catch (Exception e) {
                    logger.warn("Card parse failed: {}", e.getMessage());
                }
            }

            if (bestCard != null) {
                String productName = "Unknown";
                try {
                    productName = bestCard.findElement(By.cssSelector("div[class*='style__pro-title']")).getText();
                } catch (Exception e) {
                    logger.warn("Product name extraction failed: {}", e.getMessage());
                }

                String priceText = "₹" + bestPrice;
                try {
                    priceText = bestCard.findElement(By.cssSelector("div[class*='style__price']")).getText();
                } catch (Exception e) {
                    logger.warn("Price text fallback used");
                }

                String link = "N/A";
                try {
                    link = bestCard.findElement(By.tagName("a")).getAttribute("href");
                    if (!link.startsWith("http")) {
                        link = "https://www.1mg.com" + link;
                    }
                } catch (Exception e) {
                    logger.warn("Link extraction failed: {}", e.getMessage());
                }

                String quantity = "Unknown";
                double pricePerUnit = bestPrice;
                try {
                    String cardText = bestCard.getText();
                    Pattern quantityPattern = Pattern.compile(
                            "(strip|box|pack|bottle|jar|unit|blister|sachet|vial)\\s*(of)?\\s*(\\d+)\\s*(tablets?|capsules?|ml|mg|pcs|pieces?|units?)",
                            Pattern.CASE_INSENSITIVE);
                    Matcher matcher = quantityPattern.matcher(cardText);
                    if (matcher.find()) {
                        quantity = matcher.group().trim();
                        String numberStr = matcher.group(3);
                        int unitCount = Integer.parseInt(numberStr);
                        if (unitCount > 0) {
                            pricePerUnit = bestPrice / unitCount;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Quantity parsing failed: {}", e.getMessage());
                }

                result.put("website", "1mg");
                result.put("product", productName);
                result.put("price", priceText);
                result.put("quantity", quantity);
                result.put("buy_link", link);
                result.put("price_per_unit", String.format("₹%.2f", pricePerUnit));
            } else {
                result.put("website", "1mg");
                result.put("product", "Not available");
                result.put("price", "N/A");
                result.put("quantity", "N/A");
                result.put("buy_link", "N/A");
                result.put("price_per_unit", "N/A");
            }

        } catch (Exception e) {
            logger.error("Scraping failed: {}", e.getMessage());
            result.put("website", "1mg");
            result.put("product", "Error");
            result.put("price", "N/A");
            result.put("quantity", "N/A");
            result.put("buy_link", "N/A");
            result.put("price_per_unit", "N/A");
        } finally {
            driver.quit();
        }

        return result;
    }

    public static void main(String[] args) {
        Map<String, String> result = get1mgBestDeal("Dolo 650", "Delhi");
        logger.info("Final Output:");
        result.forEach((k, v) -> logger.info("{}: {}", k, v));
    }
}
