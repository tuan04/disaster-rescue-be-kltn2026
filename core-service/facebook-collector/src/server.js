const app = require('./app');
const { PORT } = require('./config/facebook.config');

// Entry point chỉ chịu trách nhiệm mở cổng HTTP.
app.listen(PORT, () => {
    console.log('Facebook Collector API running at:');
    console.log(`http://localhost:${PORT}`);
    console.log(`Health: http://localhost:${PORT}/health`);
    console.log(`Posts: http://localhost:${PORT}/api/facebook/posts`);
});
