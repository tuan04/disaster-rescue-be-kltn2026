// Health check không mở trình duyệt hay gọi Facebook.
function getHealth(req, res) {
    res.json({ status: 'ok' });
}

module.exports = { getHealth };
