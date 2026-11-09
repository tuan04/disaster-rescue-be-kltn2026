const { Router } = require('express');
const { getPosts } = require('../controllers/facebook.controller');

const router = Router();

// Được gắn dưới /api/facebook trong routes/index.js.
router.get('/posts', getPosts);

module.exports = router;
