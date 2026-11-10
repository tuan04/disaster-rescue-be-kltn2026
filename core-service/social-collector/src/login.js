const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { AUTH_STATE_PATH } = require('./config/facebook.config');

(async () => {
    if (!fs.existsSync(path.dirname(AUTH_STATE_PATH))) {
        fs.mkdirSync(path.dirname(AUTH_STATE_PATH), { recursive: true });
    }

    const browser = await chromium.launch({
        headless: false
    });

    const context = await browser.newContext();
    const page = await context.newPage();

    await page.goto('https://www.facebook.com/');

    console.log('Hãy đăng nhập Facebook thủ công trên trình duyệt.');
    console.log('Khi đăng nhập xong, quay lại terminal và nhấn ENTER.');

    process.stdin.once('data', async () => {
        await context.storageState({
            path: AUTH_STATE_PATH
        });

        console.log('Đã lưu session vào .auth/facebook.json');

        await browser.close();
        process.exit();
    });
})();