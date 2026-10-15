const express = require('express');
const routes = require('./routes');

const app = express();

app.use(express.json());
app.use(routes);

// Tách app khỏi listen để kiểm thử HTTP trên cổng tạm.
module.exports = app;
