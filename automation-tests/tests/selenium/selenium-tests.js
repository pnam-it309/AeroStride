const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const firefox = require('selenium-webdriver/firefox');
const assert = require('assert');

// Base URL targeting production or custom endpoint via environment variable
const BASE_URL = process.env.BASE_URL || 'https://aerostride.me';
const BROWSER = process.env.BROWSER || 'chrome';
const HEADLESS = process.env.HEADLESS !== 'false'; // Headless by default for performance

async function runTests() {
  console.log(`\n🚀 Starting Selenium WebDriver Automation Suite for AeroStride`);
  console.log(`🌍 Target Base URL: ${BASE_URL}`);
  console.log(`🌐 Browser: ${BROWSER} (Headless: ${HEADLESS})\n`);

  let driver;
  let builder = new Builder().forBrowser(BROWSER);

  // Configure Chrome options
  if (BROWSER === 'chrome') {
    const options = new chrome.Options();
    if (HEADLESS) {
      options.addArguments('--headless=new');
    }
    options.addArguments('--no-sandbox');
    options.addArguments('--disable-dev-shm-usage');
    options.addArguments('--window-size=1280,720');
    builder.setChromeOptions(options);
  }

  // Configure Firefox options
  if (BROWSER === 'firefox') {
    const options = new firefox.Options();
    if (HEADLESS) {
      options.addArguments('-headless');
    }
    builder.setFirefoxOptions(options);
  }

  driver = await builder.build();

  // Color codes for beautiful terminal report
  const green = '\x1b[32m';
  const red = '\x1b[31m';
  const reset = '\x1b[0m';
  const cyan = '\x1b[36m';

  const testResults = [];

  async function logTestResult(name, runFn) {
    console.log(`Running: ${cyan}${name}${reset}...`);
    try {
      await runFn();
      console.log(`${green}✅ PASSED: ${name}${reset}\n`);
      testResults.push({ name, status: 'PASSED' });
    } catch (err) {
      console.error(`${red}❌ FAILED: ${name}${reset}`);
      console.error(`${red}Error: ${err.message}${reset}\n`);
      testResults.push({ name, status: 'FAILED', error: err.message });
    }
  }

  try {
    // ----------------------------------------------------
    // TEST CASE 1: Verify Page Title and Hero Section
    // ----------------------------------------------------
    await logTestResult('Verify Landing Page & Hero Section Loads', async () => {
      await driver.get(BASE_URL);

      // Verify Title contains AeroStride
      const title = await driver.getTitle();
      assert.match(title, /AeroStride/i, `Page title "${title}" did not match /AeroStride/i`);

      // Verify Hero Heading is visible and contains 'AEROSTRIDE'
      const heroHeading = await driver.wait(
        until.elementLocated(By.css('h1, .text-h1, .font-weight-black')),
        15000
      );
      await driver.wait(until.elementIsVisible(heroHeading), 10000);
      const heroText = await heroHeading.getText();
      assert.match(heroText, /AEROSTRIDE/i, `Hero text "${heroText}" did not contain 'AEROSTRIDE'`);
    });

    // ----------------------------------------------------
    // TEST CASE 2: Verify Navigation Menu Elements
    // ----------------------------------------------------
    await logTestResult('Verify Navbar Navigation Elements', async () => {
      await driver.get(BASE_URL);

      const menuItems = ['Sản phẩm mới', 'Nam', 'Nữ', 'Giảm giá'];
      for (const item of menuItems) {
        // Try finding by link text or xpath
        const xpathLocator = By.xpath(`//a[contains(text(), '${item}')] | //*[contains(text(), '${item}')]`);
        const element = await driver.wait(until.elementLocated(xpathLocator), 10000);
        await driver.wait(until.elementIsVisible(element), 5000);
        const isDisplayed = await element.isDisplayed();
        assert.ok(isDisplayed, `Navbar item "${item}" is not displayed on screen`);
      }
    });

    // ----------------------------------------------------
    // TEST CASE 3: Verify "KHÁM PHÁ NGAY" Redirection
    // ----------------------------------------------------
    await logTestResult('Verify Redirection to Shop Page via CTA Button', async () => {
      await driver.get(BASE_URL);

      // Find Call to Action Button containing 'KHÁM PHÁ NGAY'
      const exploreBtnLocator = By.xpath(
        `//button[contains(translate(., 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'KHÁM PHÁ NGAY')] | ` +
        `//a[contains(translate(., 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'KHÁM PHÁ NGAY')] | ` +
        `//*[contains(text(), 'KHÁM PHÁ NGAY')]`
      );

      const exploreBtn = await driver.wait(until.elementLocated(exploreBtnLocator), 10000);
      await driver.wait(until.elementIsVisible(exploreBtn), 5000);
      
      // Click the button
      await exploreBtn.click();

      // Wait for URL to update and verify redirection to /shoes
      await driver.wait(until.urlContains('shoes'), 15000);
      const currentUrl = await driver.getCurrentUrl();
      assert.match(currentUrl, /.*shoes/, `Navigated URL "${currentUrl}" does not contain 'shoes'`);
    });

    // ----------------------------------------------------
    // TEST CASE 4: Verify Chat Widget Integration & Functionality
    // ----------------------------------------------------
    await logTestResult('Verify Customer Support Chat Widget Interaction', async () => {
      // Go back to landing page where chat is loaded
      await driver.get(BASE_URL);

      // 1. Locate Chat Floating Action Button (FAB)
      const chatFabLocator = By.css('.chat-fab, .v-btn.chat-fab');
      const chatFab = await driver.wait(until.elementLocated(chatFabLocator), 15000);
      await driver.wait(until.elementIsVisible(chatFab), 10000);

      // 2. Open Chat Window by clicking the FAB
      await chatFab.click();

      // 3. Confirm Chat Window is visible and header displayssupport label
      const chatWindowLocator = By.css('.chat-window');
      const chatWindow = await driver.wait(until.elementLocated(chatWindowLocator), 10000);
      await driver.wait(until.elementIsVisible(chatWindow), 5000);

      const chatHeader = await chatWindow.findElement(By.css('.chat-header'));
      const headerText = await chatHeader.getText();
      assert.match(headerText, /AeroStride Support/i, `Chat header "${headerText}" does not mention 'AeroStride Support'`);

      // 4. Verify Welcome Bubble is displayed with standard greeting
      const welcomeMsg = await driver.wait(until.elementLocated(By.css('.message-bubble')), 10000);
      const welcomeText = await welcomeMsg.getText();
      assert.match(welcomeText, /Xin chào! AeroStride có thể giúp gì cho bạn?/i, `Welcome message text "${welcomeText}" did not match standard chatbot greeting`);

      // 5. Fill input with a test message
      const chatInputLocator = By.css('.chat-footer input, .chat-footer textarea');
      const chatInput = await driver.findElement(chatInputLocator);
      const testMsg = 'Hello AeroStride! This is a premium E2E automation test using Selenium WebDriver.';
      await chatInput.sendKeys(testMsg);

      // 6. Send the message
      const sendBtnLocator = By.css('.chat-footer button, .chat-footer .v-btn');
      const sendBtn = await driver.findElement(sendBtnLocator);
      assert.ok(await sendBtn.isEnabled(), 'Send message button is disabled');
      await sendBtn.click();

      // 7. Verify sent message bubble appears
      const userMessageLocator = By.xpath("//div[contains(@class, 'message-group') and contains(@class, 'is-user')]//div[contains(@class, 'message-bubble')]");
      const userBubbles = await driver.wait(until.elementsLocated(userMessageLocator), 10000);
      const lastUserBubble = userBubbles[userBubbles.length - 1];
      const bubbleText = await lastUserBubble.getText();
      
      assert.strictEqual(bubbleText, testMsg, `Expected bubble text to be "${testMsg}" but got "${bubbleText}"`);
    });

  } finally {
    // Teardown browser
    if (driver) {
      await driver.quit();
    }

    // Print final premium summary report
    console.log(`====================================================`);
    console.log(`📊 SELENIUM AUTOMATION TEST REPORT SUMMARY`);
    console.log(`====================================================`);
    let passedCount = 0;
    let failedCount = 0;
    
    testResults.forEach((res, index) => {
      const bullet = res.status === 'PASSED' ? `${green}✓${reset}` : `${red}✗${reset}`;
      const statusStr = res.status === 'PASSED' ? `${green}PASSED${reset}` : `${red}FAILED${reset}`;
      console.log(`${bullet} Test ${index + 1}: ${res.name} -> ${statusStr}`);
      if (res.status === 'PASSED') passedCount++;
      else failedCount++;
    });

    console.log(`\n🏆 Total Tests: ${testResults.length} | Passed: ${green}${passedCount}${reset} | Failed: ${red}${failedCount}${reset}`);
    console.log(`====================================================\n`);

    if (failedCount > 0) {
      process.exit(1);
    } else {
      process.exit(0);
    }
  }
}

runTests().catch(err => {
  console.error('Fatal Error running Selenium Tests:', err);
  process.exit(1);
});
