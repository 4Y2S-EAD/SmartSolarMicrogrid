// Member 4: exercise the unchanged operator web UI against the real isolated QR-check API.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const root = path.resolve(__dirname, '../..');
const fixture = JSON.parse(fs.readFileSync(path.join(root, 'android/app/build/qr-device/qr-device.json'), 'utf8'));
const evidence = path.join(root, 'docs/member4-qr-evidence');
fs.mkdirSync(evidence, { recursive: true });

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    // Forward to an actual temporary API, without mocking reservation responses or changing the web API configuration.
    await page.route('http://localhost:5224/api/**', async route => {
      const response = await route.fetch({ url: route.request().url().replace('http://localhost:5224', 'http://127.0.0.1:5236') });
      await route.fulfill({ response });
    });
    await page.addInitScript(token => {
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify({ id: 'qr-check-operator', email: 'operator@example.invalid' }));
      localStorage.setItem('profile', JSON.stringify({ id: 'qr-check-operator', email: 'operator@example.invalid',
        full_name: 'Test operator', role: 'grid_operator', status: 'active' }));
    }, fixture.token);
    await page.goto('http://127.0.0.1:5174');
    const metric = page.locator('article').filter({ hasText: 'Completed' }).locator('strong');
    await metric.getByText('1', { exact: true }).waitFor();
    await page.screenshot({ path: path.join(evidence, 'web-completed-count.png'), fullPage: true });
    console.log('PASS unchanged web dashboard completed count');
    await page.getByRole('button', { name: 'Reservations', exact: true }).click();
    async function assertCompleted(name) {
      const row = page.locator('tbody tr').filter({ hasText: fixture.reservationId });
      await row.locator('.m4-status-completed').waitFor();
      assert.equal(await row.locator('.m4-status').innerText(), 'Completed');
      console.log('PASS ' + name);
    }
    await assertCompleted('all reservations reflects completion');
    await page.getByRole('button', { name: 'Completed', exact: true }).click();
    await assertCompleted('completed list reflects completion');
    await page.screenshot({ path: path.join(evidence, 'web-completed-list.png'), fullPage: true });
    await page.getByRole('button', { name: 'Booking history', exact: true }).click();
    await assertCompleted('booking history reflects completion');
    await page.getByRole('button', { name: 'Search & filter', exact: true }).click();
    await page.getByLabel('Booking ID', { exact: true }).fill(fixture.reservationId);
    await page.getByRole('button', { name: 'Search', exact: true }).click();
    await assertCompleted('search reflects completion');
    await page.screenshot({ path: path.join(evidence, 'web-completed-search.png'), fullPage: true });
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
