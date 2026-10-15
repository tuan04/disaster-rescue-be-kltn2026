const fs = require('fs');
const path = require('path');

// Tính từ vị trí file để chạy server/login ở thư mục nào cũng đúng.
const PROJECT_ROOT = path.resolve(__dirname, '../..');
const AUTH_STATE_PATH = path.join(PROJECT_ROOT, '.auth/facebook.json');
const GROUPS_PATH = path.join(PROJECT_ROOT, 'config/groups.json');

const PORT = 3000;
const MAX_POSTS_PER_GROUP = 2;
const MAX_SCROLLS = 30;
const CONCURRENCY = 2;
const POST_MESSAGE_SELECTOR =
    '[data-ad-rendering-role="story_message"], ' +
    '[data-ad-preview="message"]';

// Đọc lại mỗi lần crawl để thay groups.json không cần khởi động lại server.
function loadGroups() {
    return JSON.parse(fs.readFileSync(GROUPS_PATH, 'utf8')).groups;
}

module.exports = {
    PORT,
    AUTH_STATE_PATH,
    MAX_POSTS_PER_GROUP,
    MAX_SCROLLS,
    CONCURRENCY,
    POST_MESSAGE_SELECTOR,
    loadGroups,
};
