const { Router } = require('express');
const facebookRoutes = require('./facebook.routes');
const { getHealth } = require('../controllers/health.controller');

const router = Router();

router.get('/health', getHealth);
router.use('/api/facebook', facebookRoutes);

module.exports = router;
