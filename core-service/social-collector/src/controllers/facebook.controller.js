const facebookService = require('../services/facebook.service');

// Controller chỉ xử lý HTTP; việc mở trình duyệt và crawl thuộc service.
async function getPosts(req, res) {
    try {
        const posts = await facebookService.collectPosts();
        res.json({ total: posts.length, posts });
    } catch (error) {
        console.error('FACEBOOK COLLECT ERROR:', error.message);
        res.status(500).json({
            error: 'FACEBOOK_COLLECT_FAILED',
            message: error.message,
        });
    }
}

module.exports = { getPosts };
