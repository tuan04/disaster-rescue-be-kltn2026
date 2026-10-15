const { chromium } = require('playwright');
const {
    AUTH_STATE_PATH,
    MAX_POSTS_PER_GROUP,
    MAX_SCROLLS,
    CONCURRENCY,
    POST_MESSAGE_SELECTOR,
    loadGroups,
} = require('../config/facebook.config');
const { generatePostId, extractPostId } = require('../utils/facebook-post.util');

// Service giữ toàn bộ nghiệp vụ crawl, không phụ thuộc req/res của Express.
async function collectGroup(context, groupId) {

    const page = await context.newPage();

    const groupUrl =
        `https://www.facebook.com/groups/${groupId}`;

    const collectedPosts = new Map();

    console.log('\n======================================');
    console.log(`[${groupId}] Bắt đầu crawl`);
    console.log('======================================');

    try {

        await page.goto(
            groupUrl,
            {
                waitUntil: 'domcontentloaded',
                timeout: 60000
            }
        );

        // Chờ nội dung thực sự hiển thị thay vì đoán thời gian tải trang.
        // Nếu bị chuyển về đăng nhập hoặc DOM thay đổi, trả lỗi để phía gọi biết.
        await page.locator(POST_MESSAGE_SELECTOR)
            .first()
            .waitFor({ state: 'visible', timeout: 20000 })
            .catch(() => {
                throw new Error(
                    `[${groupId}] Không tìm thấy nội dung bài viết sau 20 giây. ` +
                    'Kiểm tra phiên đăng nhập, quyền truy cập group hoặc selector Facebook.'
                );
            });


        // ==================================================
        // COLLECT CÁC POST ĐANG HIỆN TRONG DOM
        // ==================================================

        async function collectVisiblePosts() {

            const postMessages = page.locator(
                POST_MESSAGE_SELECTOR
            );

            const count =
                await postMessages.count();

            console.log(
                `[${groupId}] DOM hiện có ${count} bài`
            );


            for (let i = 0; i < count; i++) {

                if (
                    collectedPosts.size >=
                    MAX_POSTS_PER_GROUP
                ) {
                    return;
                }

                try {

                    const messageElement =
                        postMessages.nth(i);


                    // ==================================================
                    // CLICK "XEM THÊM"
                    // ==================================================

                    const seeMore =
                        messageElement.getByText(
                            'Xem thêm',
                            {
                                exact: true
                            }
                        );

                    if (
                        await seeMore.count() > 0
                    ) {

                        await seeMore
                            .first()
                            .click()
                            .catch(() => {});

                        await page.waitForTimeout(200);
                    }


                    // ==================================================
                    // CONTENT
                    // ==================================================

                    const text =
                        await messageElement
                            .innerText()
                            .catch(() => '');

                    const cleanText = text
                        .replace(/…\s*Xem thêm/g, '')
                        .replace(/Xem thêm/g, '')
                        .replace(/Ẩn bớt/g, '')
                        .trim();


                    if (!cleanText) {
                        continue;
                    }


                    // ==================================================
                    // TÌM CONTAINER CHỨA LINKS
                    // ==================================================

                    const postContainer =
                        messageElement.locator(
                            'xpath=../../..'
                        );


                    const links =
                        await postContainer
                            .locator('a')
                            .evaluateAll(
                                anchors =>
                                    anchors
                                        .map(a => a.href)
                                        .filter(Boolean)
                            )
                            .catch(() => []);


                    // ==================================================
                    // THỬ LẤY POST ID THẬT
                    // ==================================================

                    const extracted =
                        extractPostId(links);

                    let postId;
                    let isGeneratedPostId;


                    if (extracted) {

                        postId =
                            extracted.postId;

                        isGeneratedPostId =
                            false;

                    } else {

                        postId =
                            generatePostId(
                                groupId,
                                cleanText
                            );

                        isGeneratedPostId =
                            true;

                        console.log(
                            `[${groupId}] Không có postId thật → ${postId}`
                        );
                    }


                    // ==================================================
                    // CHỐNG TRÙNG TRONG LẦN CRAWL
                    // ==================================================

                    if (
                        collectedPosts.has(postId)
                    ) {
                        continue;
                    }


                    // ==================================================
                    // POST URL
                    // ==================================================

                    const postUrl =
                        isGeneratedPostId
                            ? groupUrl
                            : `https://www.facebook.com/groups/${groupId}/posts/${postId}`;


                    // ==================================================
                    // EXTERNAL ID
                    // ==================================================

                    const externalId =
                        `FACEBOOK_GROUP:${groupId}:${postId}`;


                    // ==================================================
                    // SAVE POST
                    // ==================================================

                    const post = {

                        externalId,

                        source:
                            'FACEBOOK',

                        groupId,

                        groupUrl,

                        postId,

                        postUrl,

                        isGeneratedPostId,

                        content:
                            cleanText,

                        collectedAt:
                            new Date()
                                .toISOString()
                    };


                    collectedPosts.set(
                        postId,
                        post
                    );


                    console.log(
                        `[${groupId}] ${collectedPosts.size}/${MAX_POSTS_PER_GROUP}`
                    );

                } catch (error) {

                    console.log(
                        `[${groupId}] lỗi post ${i + 1}:`,
                        error.message
                    );
                }
            }
        }


        // ==================================================
        // SCROLL + COLLECT
        // ==================================================

        for (
            let scroll = 0;
            scroll < MAX_SCROLLS;
            scroll++
        ) {

            console.log(
                `[${groupId}] Scroll ${scroll + 1}/${MAX_SCROLLS}`
            );


            await collectVisiblePosts();


            if (
                collectedPosts.size >=
                MAX_POSTS_PER_GROUP
            ) {

                console.log(
                    `[${groupId}] Đã đủ ${MAX_POSTS_PER_GROUP} bài`
                );

                break;
            }


            await page.evaluate(() => {

                window.scrollBy({
                    top:
                        window.innerHeight * 2,

                    behavior:
                        'smooth'
                });

            });


            await page.waitForTimeout(2000);
        }


        // Collect thêm lần cuối
        if (
            collectedPosts.size <
            MAX_POSTS_PER_GROUP
        ) {

            await collectVisiblePosts();
        }

        // Không báo thành công với mảng rỗng khi các bài đều không đọc được.
        if (collectedPosts.size === 0) {
            throw new Error(
                `[${groupId}] Không đọc được nội dung bài viết nào; kiểm tra log lỗi từng bài.`
            );
        }

    } catch (error) {

        console.error(
            `[${groupId}] ERROR:`,
            error.message
        );

        // Chuyển lỗi lên API để trả HTTP 500, tránh bị hiểu là group không có bài.
        throw error;

    } finally {

        await page.close();
    }


    const posts =
        Array.from(
            collectedPosts.values()
        ).slice(
            0,
            MAX_POSTS_PER_GROUP
        );


    console.log(
        `[${groupId}] HOÀN THÀNH: ${posts.length} bài`
    );


    return posts;
}

// Mỗi request có browser riêng; luôn giải phóng tài nguyên khi thành công hoặc lỗi.
async function collectPosts() {
    let browser;
    try {
        const groupIds = loadGroups();
        console.log(`\nBắt đầu crawl ${groupIds.length} groups`);

        browser = await chromium.launch({ headless: false });
        const context = await browser.newContext({ storageState: AUTH_STATE_PATH });
        const allPosts = [];

        for (let i = 0; i < groupIds.length; i += CONCURRENCY) {
            const batch = groupIds.slice(i, i + CONCURRENCY);
            console.log(`\nBatch: ${batch.join(', ')}`);
            const results = await Promise.all(
                batch.map(groupId => collectGroup(context, groupId))
            );
            for (const posts of results) {
                allPosts.push(...posts);
            }
        }

        console.log(`TỔNG CỘNG: ${allPosts.length} POSTS`);
        return allPosts;
    } finally {
        if (browser) {
            await browser.close().catch(error => {
                console.error('Không đóng được browser:', error.message);
            });
        }
    }
}

module.exports = { collectPosts };
