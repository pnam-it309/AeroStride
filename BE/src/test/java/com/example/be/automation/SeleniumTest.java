package com.example.be.automation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class SeleniumTest {
    static WebDriver driver;

    @BeforeAll
    static void beforeAll() {
        org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        driver = new ChromeDriver(options);
    }

//    public void Login(String username, String password) {
//        By email= By.cssSelector("#input-v-1");
//        By pass=By.cssSelector("#input-v-4");
//        By submit=By.cssSelector("button[type='submit']");
//        driver.get("https://aerostride.me/admin");
//    }

    @Test
    public void case1_Login_Pass() throws Exception {
        driver.get("https://aerostride.me/admin");
        Thread.sleep(2000);

        driver.findElement(By.cssSelector("#input-v-1")).sendKeys("phitrang082006@gmail.com");
        Thread.sleep(2000);
        driver.findElement(By.cssSelector("#input-v-4")).sendKeys("123456");
        Thread.sleep(2000);

        WebElement element = driver.findElement(By.cssSelector("button[type='submit']"));
        element.click();
        Thread.sleep(5000);

        driver.findElement(By.xpath("//*[text()='Quản lý phiếu giảm giá']")).click();
        Thread.sleep(5000);

        WebElement elementB = driver.findElement(By.cssSelector(".v-btn__content"));
        elementB.click();
        Thread.sleep(5000);

    }

//    @Test
//    public void case2_Login_Fail_Sai_Mat_Khau() throws Exception {
//        driver.get("https://aerostride.me/admin");
//        Thread.sleep(2000);
//
//        driver.findElement(By.cssSelector("#input-v-1")).sendKeys("phitrang082006@gmail.com");
//        Thread.sleep(2000);
//        driver.findElement(By.cssSelector("#input-v-4")).sendKeys("12345");
//        Thread.sleep(2000);
//
//        WebElement element = driver.findElement(By.cssSelector("button[type='submit']"));
//        element.click();
//        Thread.sleep(2000);
//
//
//    }
//
//    @Test
//    public void case3_Login_Fail_Sai_Gmail() throws Exception {
//        driver.get("https://aerostride.me/admin");
//        Thread.sleep(2000);
//
//        driver.findElement(By.cssSelector("#input-v-1")).sendKeys("phitrang082006@gmail");
//        Thread.sleep(2000);
//        driver.findElement(By.cssSelector("#input-v-4")).sendKeys("123456");
//        Thread.sleep(2000);
//
//        WebElement element = driver.findElement(By.cssSelector("button[type='submit']"));
//        element.click();
//        Thread.sleep(2000);
//
//    }
//
//    @Test
//    public void case4_Login_Fail_Bo_Trong_Email() throws Exception {
//        driver.get("https://aerostride.me/admin");
//        Thread.sleep(2000);
//
//        driver.findElement(By.cssSelector("#input-v-1")).sendKeys("");
//        Thread.sleep(2000);
//        driver.findElement(By.cssSelector("#input-v-4")).sendKeys("123456");
//        Thread.sleep(2000);
//
//        WebElement element = driver.findElement(By.cssSelector("button[type='submit']"));
//        element.click();
//        Thread.sleep(2000);
//    }
//    public void case4_Login_Fail_Bo_Trong_Password() throws Exception {
//        driver.get("https://aerostride.me/admin");
//        Thread.sleep(2000);
//
//        driver.findElement(By.cssSelector("#input-v-1")).sendKeys("phitrang082006@gmail.com");
//        Thread.sleep(2000);
//        driver.findElement(By.cssSelector("#input-v-4")).sendKeys("");
//        Thread.sleep(2000);
//
//        WebElement element = driver.findElement(By.cssSelector("button[type='submit']"));
//        element.click();
//        Thread.sleep(2000);
//    }



    @AfterAll
    public static void afterAll() {
        driver.quit();
    }
}
