const { test, expect } = require('@playwright/test');

test.describe('AeroStride E2E: Landing Page & Shop Navigation', () => {
  
  test.beforeEach(async ({ page }) => {
    // Navigate to the deployed landing page
    await page.goto('/');
  });

  test('should load the landing page successfully and verify page title', async ({ page }) => {
    // Verify Page Title containing AeroStride
    await expect(page).toHaveTitle(/AeroStride/i);
    
    // Verify Hero Heading is visible
    const heroHeading = page.locator('h1, .text-h1, .font-weight-black');
    await expect(heroHeading.first()).toBeVisible();
    await expect(heroHeading.first()).toContainText(/AEROSTRIDE/i);
  });

  test('should display Navigation Bar menu items', async ({ page }) => {
    // Verify navbar navigation links are present
    const navLinks = ['Sản phẩm mới', 'Nam', 'Nữ', 'Giảm giá'];
    for (const linkText of navLinks) {
      const link = page.getByRole('link', { name: linkText }).or(page.locator(`text=${linkText}`));
      await expect(link.first()).toBeVisible();
    }
  });

  test('should have a functional "KHÁM PHÁ NGAY" action button leading to shoes page', async ({ page }) => {
    // Find the primary call to action button
    const exploreBtn = page.getByRole('button', { name: /KHÁM PHÁ NGAY/i })
      .or(page.locator('text=KHÁM PHÁ NGAY'))
      .or(page.locator('a:has-text("KHÁM PHÁ NGAY")'));

    await expect(exploreBtn.first()).toBeVisible();
    
    // Click the button to explore
    await exploreBtn.first().click();

    // Verify it navigates to the listing page /shoes
    await expect(page).toHaveURL(/.*shoes/);
  });
});
