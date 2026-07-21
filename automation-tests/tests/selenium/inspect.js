const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');

// Same override the other selenium suites use, so this can be pointed at a local
// stack instead of always hitting the deployed site.
const BASE_URL = process.env.BASE_URL || 'https://aerostride.me';

async function run() {
  const options = new chrome.Options();
  options.addArguments('--headless=new');
  options.addArguments('--no-sandbox');
  options.addArguments('--disable-dev-shm-usage');
  options.addArguments('--window-size=1280,1024');

  const driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build();

  try {
    console.log(`Navigating to landing page: ${BASE_URL}`);
    await driver.get(BASE_URL);
    await driver.sleep(5000);

    console.log('Triggering client-side navigation to /admin/login via Vue Router...');
    await driver.executeScript(() => {
      const appEl = document.getElementById('app');
      if (appEl && appEl.__vue_app__ && appEl.__vue_app__.config.globalProperties.$router) {
        appEl.__vue_app__.config.globalProperties.$router.push('/admin/login');
      } else {
        throw new Error('Vue Router not found on #app!');
      }
    });

    console.log('Waiting 5s for /admin/login to render...');
    await driver.sleep(5000);

    const title = await driver.getTitle();
    console.log('Title:', title);

    const url = await driver.getCurrentUrl();
    console.log('Current URL:', url);

    console.log('Searching for input elements on /admin/login...');
    const inputs = await driver.findElements(By.css('input'));
    console.log(`Found ${inputs.length} inputs:`);
    for (const input of inputs) {
      const type = await input.getAttribute('type');
      const placeholder = await input.getAttribute('placeholder');
      const outerHTML = await input.getAttribute('outerHTML');
      console.log(`- type="${type}", placeholder="${placeholder}" -> ${outerHTML}`);
    }

  } catch (e) {
    console.error('Error:', e);
  } finally {
    await driver.quit();
  }
}

run();
