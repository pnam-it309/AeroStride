const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const firefox = require('selenium-webdriver/firefox');
const assert = require('assert');

// Target URL and Browser settings
const BASE_URL = process.env.BASE_URL || 'https://aerostride.me';
const BROWSER = process.env.BROWSER || 'chrome';
const HEADLESS = process.env.HEADLESS !== 'false'; // Headless by default for fast pipelines

async function runAdminManagementTests() {
  console.log(`\n👑 STARTING COMPLETE ADMIN MANAGEMENT FUNCTIONAL AUTOMATION TESTS`);
  console.log(`🌍 Target Base URL: ${BASE_URL}`);
  console.log(`🌐 Browser: ${BROWSER} (Headless: ${HEADLESS})\n`);

  let driver;
  let builder = new Builder().forBrowser(BROWSER);

  if (BROWSER === 'chrome') {
    const options = new chrome.Options();
    if (HEADLESS) {
      options.addArguments('--headless=new');
    }
    options.addArguments('--no-sandbox');
    options.addArguments('--disable-dev-shm-usage');
    options.addArguments('--window-size=1280,1024'); // High resolution for full Vuetify UI rendering
    builder.setChromeOptions(options);
  }

  if (BROWSER === 'firefox') {
    const options = new firefox.Options();
    if (HEADLESS) {
      options.addArguments('-headless');
    }
    builder.setFirefoxOptions(options);
  }

  driver = await builder.build();

  // Terminal Colors for Beautiful Console Reports
  const green = '\x1b[32m';
  const red = '\x1b[31m';
  const reset = '\x1b[0m';
  const cyan = '\x1b[36m';
  const yellow = '\x1b[33m';
  const magenta = '\x1b[35m';

  const testModules = [
    {
      id: 1,
      name: 'Quản lý sản phẩm (Product Management)',
      path: '/admin/san-pham',
      expectedTitle: 'Danh sách sản phẩm'
    },
    {
      id: 2,
      name: 'Quản lý biến thể (Variant Management)',
      path: '/admin/san-pham/bien-the',
      expectedTitle: 'Danh mục biến thể'
    },
    {
      id: 3,
      name: 'Quản lý thuộc tính (Attribute Management)',
      path: '/admin/thuoc-tinh/thuong-hieu',
      expectedTitle: 'Danh sách Thương hiệu'
    },
    {
      id: 4,
      name: 'Quản lý phiếu giảm giá (Voucher Management)',
      path: '/admin/phieu-giam-gia',
      expectedTitle: 'Danh sách phiếu giảm giá'
    },
    {
      id: 5,
      name: 'Quản lý đợt giảm giá (Discount Management)',
      path: '/admin/dot-giam-gia',
      expectedTitle: 'Danh sách đợt giảm giá'
    },
    {
      id: 6,
      name: 'Quản lý khách hàng (Customer Management)',
      path: '/admin/khach-hang',
      expectedTitle: 'Danh sách khách hàng'
    },
    {
      id: 7,
      name: 'Quản lý nhân viên (Staff Management)',
      path: '/admin/nhan-vien',
      expectedTitle: 'Danh sách nhân viên'
    },
    {
      id: 8,
      name: 'Quản lý hóa đơn (Invoice Management)',
      path: '/admin/hoa-don',
      expectedTitle: 'Danh sách hóa đơn'
    }
  ];

  // High-reliability helper to navigate client-side via Vue Router
  // This bypasses relative assets resolution issue on hard page reload
  async function navigateClientSide(targetPath) {
    console.log(`  ✈️ Navigating client-side to: ${targetPath}...`);
    await driver.executeScript((path) => {
      const appEl = document.getElementById('app');
      if (appEl && appEl.__vue_app__ && appEl.__vue_app__.config.globalProperties.$router) {
        appEl.__vue_app__.config.globalProperties.$router.push(path);
      } else {
        throw new Error('Vue Router instance not found on #app!');
      }
    }, targetPath);
  }

  try {
    // ----------------------------------------------------
    // STEP 1: LOAD MAIN WEB AND NAVIGATE CLIENT-SIDE TO LOGIN
    // ----------------------------------------------------
    console.log(`🔑 ${cyan}Step 1: Admin Authentication...${reset}`);
    console.log(`Opening Landing Page to initialize Vue SPA: ${BASE_URL}`);
    await driver.get(BASE_URL);

    // Wait for the Vue app to load and be ready
    console.log('Waiting for Vue instance to initialize...');
    await driver.wait(async () => {
      return await driver.executeScript(() => {
        const appEl = document.getElementById('app');
        return !!(appEl && appEl.__vue_app__);
      });
    }, 15000, 'Vue SPA failed to mount within 15 seconds.');

    // Route to Login client-side
    await navigateClientSide('/admin/login');

    // Locate username input
    console.log('Locating username and password inputs...');
    const usernameInputLocator = By.css('input[placeholder*="Nhập username"], input[type="text"]');
    const usernameInput = await driver.wait(until.elementLocated(usernameInputLocator), 10000);
    await driver.wait(until.elementIsVisible(usernameInput), 5000);

    // Locate password input
    const passwordInputLocator = By.css('input[type="password"]');
    const passwordInput = await driver.findElement(passwordInputLocator);

    // Send credentials (using bootstrap info)
    console.log('Sending credentials to login form...');
    await usernameInput.sendKeys('nguyenhuyducbg19062002@gmail.com');
    await passwordInput.sendKeys('123456');

    // Click Login submit button
    const submitBtnLocator = By.css('button[type="submit"], .login-btn');
    const submitBtn = await driver.findElement(submitBtnLocator);
    console.log('Submitting login form...');
    await submitBtn.click();

    // Wait for redirect to admin statistic / statistics dashboard
    console.log('Waiting for redirection to admin dashboard (/admin/thong-ke)...');
    await driver.wait(until.urlContains('/admin/thong-ke'), 20000);
    console.log(`${green}✓ Authentication successful! Reached Dashboard.${reset}\n`);

    // ----------------------------------------------------
    // STEP 2: LOOP THROUGH THE 8 ADMIN MODULES
    // ----------------------------------------------------
    console.log(`🖥️ ${cyan}Step 2: Automating Multi-Module Management Flow Validation...${reset}\n`);

    for (const mod of testModules) {
      console.log(`----------------------------------------------------------------------`);
      console.log(`🏃 Running Test Case ${mod.id}: ${magenta}${mod.name}${reset}`);
      
      // Perform client-side routing to the module
      await navigateClientSide(mod.path);

      // Verify the URL was updated
      await driver.wait(until.urlContains(mod.path.split('/')[2]), 10000);
      const currentUrl = await driver.getCurrentUrl();
      console.log(`  📍 Current URL: ${currentUrl}`);

      // 1. Locate and check the AdminTable Title Header
      console.log(`  🔍 Verifying table title matches: "${mod.expectedTitle}"...`);
      const titleHeaderLocator = By.xpath(
        `//h3[contains(text(), "${mod.expectedTitle}")] | ` +
        `//*[contains(@class, "table-toolbar")]//*[contains(text(), "${mod.expectedTitle}")]`
      );
      const titleHeader = await driver.wait(until.elementLocated(titleHeaderLocator), 15000);
      await driver.wait(until.elementIsVisible(titleHeader), 10000);
      const headerText = await titleHeader.getText();
      console.log(`  ✨ Header found: "${headerText}"`);
      assert.ok(headerText.includes(mod.expectedTitle), `Page header "${headerText}" does not contain expected title "${mod.expectedTitle}"`);

      // 2. Locate the Native Admin Table component
      console.log('  🔍 Verifying AdminTable presence...');
      const adminTableLocator = By.css('.native-admin-table, table, .balanced-table table');
      const adminTable = await driver.wait(until.elementLocated(adminTableLocator), 15000);
      await driver.wait(until.elementIsVisible(adminTable), 10000);

      // 3. Wait for Vuetify asynchronously loaded content to finish rendering (spinner goes away)
      console.log('  ⏳ Waiting for API loading spinner to finish...');
      try {
        await driver.wait(async () => {
          const loaders = await driver.findElements(By.css('.v-progress-circular'));
          return loaders.length === 0;
        }, 15000);
        console.log('  ✓ API loader finished.');
      } catch (e) {
        console.log('  ⚠️ Loader spinner did not disappear or was not found, continuing validation...');
      }

      // 4. Check row elements in the native table body
      console.log('  📊 Counting data rows in table...');
      const rowLocator = By.css('.native-admin-table tbody tr.data-row, table tbody tr.data-row, table tbody tr');
      const rows = await driver.findElements(rowLocator);
      
      // Some modules might have an empty state if there's no seed data, check if empty state renders or if rows are loaded
      if (rows.length === 1) {
        const rowText = await rows[0].getText();
        if (rowText.includes('Không có dữ liệu') || rowText.includes('Đang tải dữ liệu')) {
          console.log(`  ℹ️ Table rendered successfully with standard placeholder message: "${rowText.trim()}"`);
        } else {
          console.log(`  ✓ Table rendered 1 data row successfully.`);
        }
      } else {
        console.log(`  ✓ Table rendered ${rows.length} rows successfully.`);
      }

      mod.status = 'PASSED';
      console.log(`${green}✓ Module [${mod.name}] validation PASSED!${reset}`);
    }

  } catch (err) {
    console.error(`\n❌ Error running Admin Management Automation Tests:`, err);
    process.exit(1);
  } finally {
    if (driver) {
      await driver.quit();
    }

    console.log(`\n======================================================================`);
    console.log(`📊 ADMIN MANAGEMENT MULTI-MODULE TEST RESULTS SUMMARY`);
    console.log(`======================================================================`);
    testModules.forEach(mod => {
      const bullet = mod.status === 'PASSED' ? `${green}✓${reset}` : `${red}✗${reset}`;
      const statusText = mod.status === 'PASSED' ? `${green}PASSED${reset}` : `${red}FAILED${reset}`;
      console.log(`${bullet} Test ${mod.id}: ${mod.name} -> ${statusText}`);
    });
    console.log(`======================================================================`);
    const passedCount = testModules.filter(m => m.status === 'PASSED').length;
    console.log(`🏆 Total Modules verified: ${testModules.length} | Passed: ${green}${passedCount}${reset} | Failed: ${red}${testModules.length - passedCount}${reset}`);
    console.log(`======================================================================\n`);

    if (passedCount === testModules.length) {
      process.exit(0);
    } else {
      process.exit(1);
    }
  }
}

runAdminManagementTests();
