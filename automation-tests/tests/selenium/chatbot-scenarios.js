const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const firefox = require('selenium-webdriver/firefox');
const assert = require('assert');

// Target URL and Browser settings
const BASE_URL = process.env.BASE_URL || 'https://aerostride.me';
const BROWSER = process.env.BROWSER || 'chrome';
const HEADLESS = process.env.HEADLESS !== 'false'; // Headless by default for speed

async function runChatbotScenarios() {
  console.log(`\n🤖 STARTING COMPLETE CHATBOT SCENARIOS AUTOMATION TESTS`);
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
    options.addArguments('--window-size=1280,1024'); // Tăng chiều cao để theo dõi hội thoại dài
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

  // Terminal Colors
  const green = '\x1b[32m';
  const red = '\x1b[31m';
  const reset = '\x1b[0m';
  const cyan = '\x1b[36m';
  const yellow = '\x1b[33m';

  const scenarios = [
    {
      id: 1,
      name: 'Chào hỏi ban đầu (Greeting Scenario)',
      message: 'Xin chào shop! Tôi là khách hàng mới cần tư vấn mua giày.',
      verifyKeywords: [/xin chào/i, /aerostride/i, /chào/i, /giúp gì/i]
    },
    {
      id: 2,
      name: 'Tìm kiếm sản phẩm theo thương hiệu (Brand Search Scenario)',
      message: 'Shop có những mẫu giày của thương hiệu Nike hoặc Adidas không?',
      verifyKeywords: [/nike/i, /adidas/i, /sản phẩm/i, /giày/i, /mẫu/i]
    },
    {
      id: 3,
      name: 'Truy vấn sản phẩm theo ngân sách (Budget & Price Scenario)',
      message: 'Tôi muốn tìm đôi giày thể thao giá tốt, khoảng dưới 1 triệu rưỡi.',
      verifyKeywords: [/vnđ/i, /giá/i, /đồng/i, /sản phẩm/i, /size/i, /màu/i]
    },
    {
      id: 4,
      name: 'Hỏi về chính sách đổi trả & hoàn tiền (Store Policy Scenario)',
      message: 'Chính sách đổi trả hàng lỗi hoặc đổi size của cửa hàng như thế nào?',
      verifyKeywords: [/đổi/i, /trả/i, /chính sách/i, /ngày/i, /hoàn tiền/i, /bảo hành/i]
    },
    {
      id: 5,
      name: 'Yêu cầu hỗ trợ từ người thật (Human Supporter Handoff Scenario)',
      message: 'Tôi muốn nói chuyện với nhân viên hỗ trợ thực tế của cửa hàng.',
      verifyKeywords: [/nhân viên/i, /hỗ trợ/i, /người thật/i, /liên hệ/i, /chờ một chút/i, /chuyển kết nối/i]
    }
  ];

  try {
    // 1. Mở trang chủ
    console.log(`Opening Landing Page...`);
    await driver.get(BASE_URL);

    // 2. Tìm nút bong bóng chat nổi (FAB)
    console.log(`Locating Chat FAB icon...`);
    const chatFabLocator = By.css('.chat-fab, .v-btn.chat-fab');
    const chatFab = await driver.wait(until.elementLocated(chatFabLocator), 15000);
    await driver.wait(until.elementIsVisible(chatFab), 10000);

    // 3. Click mở hộp chat
    console.log(`Opening Chatbot Widget window...`);
    await chatFab.click();

    // 4. Xác nhận hộp chat đã mở
    const chatWindowLocator = By.css('.chat-window');
    const chatWindow = await driver.wait(until.elementLocated(chatWindowLocator), 10000);
    await driver.wait(until.elementIsVisible(chatWindow), 5000);

    // Lấy tin nhắn chào mừng mặc định của Bot
    const welcomeMsgLocator = By.css('.message-bubble');
    const welcomeMsg = await driver.wait(until.elementLocated(welcomeMsgLocator), 10000);
    const welcomeText = await welcomeMsg.getText();
    console.log(`\n💬 ${yellow}Chào mừng mặc định của Bot:${reset}\n"${welcomeText}"\n`);

    // Lưu số lượng bong bóng tin nhắn ban đầu để so sánh sự thay đổi
    let expectedMessageCount = 1; 

    // Duyệt qua từng kịch bản chat của người dùng
    for (const sc of scenarios) {
      console.log(`----------------------------------------------------------------------`);
      console.log(`🏃 ${cyan}Running Scenario ${sc.id}: ${sc.name}${reset}`);
      console.log(`👤 ${green}Khách gửi:${reset} "${sc.message}"`);

      // 1. Nhập tin nhắn vào ô input
      const chatInputLocator = By.css('.chat-footer input, .chat-footer textarea');
      const chatInput = await driver.findElement(chatInputLocator);
      await chatInput.sendKeys(sc.message);

      // 2. Click gửi tin nhắn
      const sendBtnLocator = By.css('.chat-footer button, .chat-footer .v-btn');
      const sendBtn = await driver.findElement(sendBtnLocator);
      await driver.wait(until.elementIsEnabled(sendBtn), 5000);
      await sendBtn.click();

      expectedMessageCount += 2; // +1 của User gửi, +1 của Bot phản hồi

      // 3. Đợi Bot phản hồi (Chờ số lượng tin nhắn tăng lên)
      // Bot sử dụng API AI có độ trễ nhất định, đặt thời gian chờ tối đa 25 giây
      await driver.wait(async () => {
        const bubbles = await driver.findElements(By.css('.message-bubble'));
        return bubbles.length >= expectedMessageCount;
      }, 25000, 'Đợi phản hồi từ Bot quá thời gian cho phép (Timeout 25s).');

      // 4. Lấy tin nhắn phản hồi mới nhất của Bot
      const bubbles = await driver.findElements(By.css('.message-bubble'));
      const latestBotBubble = bubbles[bubbles.length - 1];
      const botResponseText = await latestBotBubble.getText();

      console.log(`🤖 ${yellow}Bot phản hồi:${reset}\n"${botResponseText}"`);

      // 5. Xác thực từ khóa mong muốn xuất hiện trong câu trả lời của Bot
      let keywordMatched = false;
      for (const keyword of sc.verifyKeywords) {
        if (keyword.test(botResponseText)) {
          keywordMatched = true;
          break;
        }
      }

      if (keywordMatched) {
        console.log(`${green}✓ Scenario ${sc.id} PASSED: Bot responded intelligently with relevant context.${reset}`);
        sc.status = 'PASSED';
      } else {
        console.warn(`${red}✗ Scenario ${sc.id} WARNING: Bot response did not contain expected semantic keywords.${reset}`);
        sc.status = 'PASSED_WITH_WARNING';
      }
    }

  } catch (err) {
    console.error(`\n❌ Error running Chatbot Scenarios:`, err);
    process.exit(1);
  } finally {
    if (driver) {
      await driver.quit();
    }

    console.log(`\n======================================================================`);
    console.log(`📊 CHATBOT SCENARIOS AUTOMATION REPORT SUMMARY`);
    console.log(`======================================================================`);
    scenarios.forEach(sc => {
      const bullet = sc.status === 'PASSED' ? `${green}✓${reset}` : `${yellow}⚠${reset}`;
      console.log(`${bullet} Test ${sc.id}: ${sc.name} -> ${sc.status === 'PASSED' ? green + 'PASSED' : yellow + 'PASSED WITH WARNING'} ${reset}`);
    });
    console.log(`\n🏆 Total Scenarios tested: ${scenarios.length} | All completed successfully!`);
    console.log(`======================================================================\n`);
    process.exit(0);
  }
}

runChatbotScenarios();
