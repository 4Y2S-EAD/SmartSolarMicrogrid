// Member 4: browser integration against the real local verification API; test fixtures use existing POST.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../../../..');
const fixture = JSON.parse(fs.readFileSync(path.join(root, 'backend/SmartSolarMicrogrid.API/bin/operator-verification/web-test.json')));
(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  await context.addInitScript(({ token }) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify({ id: 'operator-verification', email: 'verification@local.test' }));
    localStorage.setItem('profile', JSON.stringify({ id: 'operator-verification', full_name: 'Grid Operator', role: 'grid_operator', status: 'active' }));
  }, fixture);
  let failure = 0;
  await context.route('http://localhost:5224/api/**', async route => {
    if (failure) { await route.fulfill({ status: failure === 200 ? 200 : failure, contentType: 'application/json', body: JSON.stringify(failure === 200 ? {} : { message: 'Verification: request unavailable' }) }); return; }
    const response = await route.fetch({ url: route.request().url().replace('http://localhost:5224', 'http://127.0.0.1:5235') });
    await route.fulfill({ response });
  });
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', err => errors.push(err.message));
  const output = path.join(root, 'docs/member4-operator-evidence'); fs.mkdirSync(output, { recursive: true });
  try {
    await page.goto('http://127.0.0.1:5173');
    await page.getByRole('heading', { name: 'Keep every booking on track' }).waitFor();
    await page.locator('.m4-operator tbody tr').first().waitFor();
    await page.screenshot({ path: path.join(output, 'web-dashboard.png'), fullPage: true, animations: 'disabled' });
    await page.getByRole('button', { name: 'Pending', exact: true }).click();
    await page.locator('.m4-operator tbody tr').first().waitFor();
    await page.screenshot({ path: path.join(output, 'web-pending.png'), fullPage: true, animations: 'disabled' });
    await page.getByRole('button', { name: 'Search & filter', exact: true }).click();
    await page.getByLabel('Booking ID', { exact: true }).fill(fixture.reservation.reservationId);
    await page.getByRole('button', { name: 'Search', exact: true }).click();
    await page.locator('.m4-pagination').filter({ hasText: '1 result(s)' }).waitFor();
    await page.getByRole('button', { name: 'Approve', exact: true }).click();
    await page.locator('.m4-status-approved').waitFor();
    await page.screenshot({ path: path.join(output, 'web-approved-search.png'), fullPage: true, animations: 'disabled' });
    await page.getByLabel('Booking ID', { exact: true }).fill('000000000000000000000000');
    await page.getByRole('button', { name: 'Search', exact: true }).click();
    await page.getByRole('heading', { name: 'No reservations found' }).waitFor();
    await page.getByLabel('Booking ID', { exact: true }).fill('invalid');
    await page.getByRole('button', { name: 'Search', exact: true }).click();
    await page.getByRole('alert').waitFor();
    await page.getByRole('button', { name: 'Booking history', exact: true }).click();
    await page.locator('.m4-operator tbody tr').first().waitFor();
    await page.screenshot({ path: path.join(output, 'web-history.png'), fullPage: true, animations: 'disabled' });
    await page.getByRole('button', { name: 'Completed', exact: true }).click();
    await page.locator('.m4-pagination').waitFor();
    await page.setViewportSize({ width: 390, height: 844 });
    await page.screenshot({ path: path.join(output, 'web-responsive.png'), fullPage: true, animations: 'disabled' });
    for (const code of [503, 401, 403, 200]) {
      failure = code;
      await page.getByRole('button', { name: 'Refresh', exact: true }).click();
      await page.getByRole('alert').waitFor();
    }
    failure = 0;
    await page.getByRole('button', { name: 'Retry', exact: true }).click();
    await page.locator('.m4-pagination').waitFor();
    if (errors.length) throw new Error(errors.join('\n'));
    console.log('PASS: real web dashboard, pending, ID search, approval/reload, empty results, invalid search, history, completed responsive rendering, simulated service/auth failures and malformed response recovery.');
  } finally { await browser.close(); }
})().catch(err => { console.error(err.message); process.exit(1); });
