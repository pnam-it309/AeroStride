const { test, expect } = require('@playwright/test');

test.describe('AeroStride E2E: Customer Chat Widget Integration', () => {

  test.beforeEach(async ({ page }) => {
    // Open the homepage where chat is globally embedded
    await page.goto('/');
  });

  test('should load the chat FAB and handle opening the chat box', async ({ page }) => {
    // Wait for the floating action button (FAB) icon to appear
    const chatFab = page.locator('.chat-fab').or(page.locator('.v-btn.chat-fab'));
    await expect(chatFab).toBeVisible({ timeout: 15000 });

    // Click the FAB to open the chat window
    await chatFab.click();

    // Verify Chat Window header is displayed
    const chatWindow = page.locator('.chat-window');
    await expect(chatWindow).toBeVisible();
    await expect(chatWindow.locator('.chat-header')).toContainText(/AeroStride Support/i);
  });

  test('should display default chatbot greeting message and accept user message inputs', async ({ page }) => {
    // Open Chat Window
    const chatFab = page.locator('.chat-fab');
    await chatFab.click();

    // Verify default welcome bubble is visible
    const welcomeMsg = page.locator('.message-bubble').first();
    await expect(welcomeMsg).toBeVisible();
    await expect(welcomeMsg).toContainText(/Xin chào! AeroStride có thể giúp gì cho bạn?/i);

    // Find message textarea/input field
    const chatInput = page.locator('.chat-footer input').or(page.locator('.chat-footer textarea')).first();
    await expect(chatInput).toBeVisible();

    // Type test message
    await chatInput.fill('Hello AeroStride! This is an E2E automation test.');

    // Find send button and click it
    const sendBtn = page.locator('.chat-footer button').or(page.locator('.chat-footer .v-btn')).last();
    await expect(sendBtn).toBeEnabled();
    await sendBtn.click();

    // Verify sent message is displayed as user's bubble
    const userMessages = page.locator('.message-group.is-user .message-bubble');
    await expect(userMessages.last()).toBeVisible();
    await expect(userMessages.last()).toContainText('Hello AeroStride! This is an E2E automation test.');
  });
});
