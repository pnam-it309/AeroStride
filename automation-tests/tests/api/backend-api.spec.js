const { test, expect } = require('@playwright/test');

test.describe('AeroStride Backend API Automation Tests', () => {

  test('GET /api/v1/customer/landing/products - Should fetch public products with OK status', async ({ request }) => {
    // Call the public product landing endpoint
    const response = await request.get('/api/v1/customer/landing/products', {
      params: {
        size: 6
      }
    });

    // Verify response status code is 200 (OK)
    expect(response.status()).toBe(200);

    // Verify content type headers
    expect(response.headers()['content-type']).toContain('application/json');

    // Parse response body
    const body = await response.json();
    
    // Verify properties and structure
    expect(body).toHaveProperty('success', true);
    expect(body).toHaveProperty('data');
    expect(Array.isArray(body.data)).toBe(true);

    // If there are products, check core fields are valid
    if (body.data.length > 0) {
      const product = body.data[0];
      expect(product).toHaveProperty('id');
      expect(product).toHaveProperty('tenSanPham');
      expect(product).toHaveProperty('hinhAnh');
    }
  });

  test('POST /api/v1/auth/login - Should fail with 401 Unauthorized for incorrect credentials', async ({ request }) => {
    // Send POST payload with incorrect credentials
    const response = await request.post('/api/v1/auth/login', {
      data: {
        username: 'non_existent_user_9999',
        password: 'wrong_password_xyz',
        loginType: 'ADMIN'
      }
    });

    // Verify response status code is 401 (Unauthorized) or 400 depending on exact logic
    // We expect it to be unauthorized (401)
    expect(response.status() === 401 || response.status() === 400).toBe(true);

    const body = await response.json();
    
    // Verify error format
    expect(body).toHaveProperty('success', false);
    expect(body).toHaveProperty('message');
  });
});
