# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: ui\landing.spec.js >> AeroStride E2E: Landing Page & Shop Navigation >> should load the landing page successfully and verify page title
- Location: tests\ui\landing.spec.js:10:3

# Error details

```
Error: expect(locator).toContainText(expected) failed

Locator: locator('h1, .text-h1, .font-weight-black').first()
Timeout: 5000ms
Expected pattern: /AEROSTRIDE/i
Received string:  "
                Web server is down
                Error code 521
            "

Call log:
  - Expect "toContainText" with timeout 5000ms
  - waiting for locator('h1, .text-h1, .font-weight-black').first()
    14 × locator resolved to <h1 class="inline-block sm:block sm:mb-2 font-light text-60 lg:text-4xl text-black-dark leading-tight mr-2">…</h1>
       - unexpected value "
                Web server is down
                Error code 521
            "

```

```yaml
- heading "Web server is down Error code 521" [level=1]
```

# Test source

```ts
  1  | const { test, expect } = require('@playwright/test');
  2  | 
  3  | test.describe('AeroStride E2E: Landing Page & Shop Navigation', () => {
  4  |   
  5  |   test.beforeEach(async ({ page }) => {
  6  |     // Navigate to the deployed landing page
  7  |     await page.goto('/');
  8  |   });
  9  | 
  10 |   test('should load the landing page successfully and verify page title', async ({ page }) => {
  11 |     // Verify Page Title containing AeroStride
  12 |     await expect(page).toHaveTitle(/AeroStride/i);
  13 |     
  14 |     // Verify Hero Heading is visible
  15 |     const heroHeading = page.locator('h1, .text-h1, .font-weight-black');
  16 |     await expect(heroHeading.first()).toBeVisible();
> 17 |     await expect(heroHeading.first()).toContainText(/AEROSTRIDE/i);
     |                                       ^ Error: expect(locator).toContainText(expected) failed
  18 |   });
  19 | 
  20 |   test('should display Navigation Bar menu items', async ({ page }) => {
  21 |     // Verify navbar navigation links are present
  22 |     const navLinks = ['New & Featured', 'Men', 'Women', 'Sale'];
  23 |     for (const linkText of navLinks) {
  24 |       const link = page.getByRole('link', { name: linkText }).or(page.locator(`text=${linkText}`));
  25 |       await expect(link.first()).toBeVisible();
  26 |     }
  27 |   });
  28 | 
  29 |   test('should have a functional "KHÁM PHÁ NGAY" action button leading to shoes page', async ({ page }) => {
  30 |     // Find the primary call to action button
  31 |     const exploreBtn = page.getByRole('button', { name: /KHÁM PHÁ NGAY/i })
  32 |       .or(page.locator('text=KHÁM PHÁ NGAY'))
  33 |       .or(page.locator('a:has-text("KHÁM PHÁ NGAY")'));
  34 | 
  35 |     await expect(exploreBtn.first()).toBeVisible();
  36 |     
  37 |     // Click the button to explore
  38 |     await exploreBtn.first().click();
  39 | 
  40 |     // Verify it navigates to the listing page /shoes
  41 |     await expect(page).toHaveURL(/.*shoes/);
  42 |   });
  43 | });
  44 | 
```