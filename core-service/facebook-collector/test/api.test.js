const { test, before, after, afterEach, mock } = require('node:test');
const assert = require('node:assert/strict');
const { once } = require('node:events');
const { chromium } = require('playwright');
const app = require('../src/app');
const facebookService = require('../src/services/facebook.service');

let server;
let baseUrl;

before(async () => {
    server = app.listen(0, '127.0.0.1');
    await once(server, 'listening');
    baseUrl = `http://127.0.0.1:${server.address().port}`;
});

afterEach(() => mock.restoreAll());

after(async () => {
    server.closeAllConnections();
    await new Promise((resolve, reject) => {
        server.close(error => error ? reject(error) : resolve());
    });
});

test('GET /health returns ok without opening a browser', async () => {
    const launch = mock.method(chromium, 'launch', async () => {
        throw new Error('Health check must not launch Chromium');
    });
    const response = await fetch(`${baseUrl}/health`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { status: 'ok' });
    assert.equal(launch.mock.callCount(), 0);
});

test('GET /api/facebook/posts preserves the response contract', async () => {
    const posts = [{
        externalId: 'FACEBOOK_GROUP:test:1',
        source: 'FACEBOOK',
        groupId: 'test',
        groupUrl: 'https://www.facebook.com/groups/test',
        postId: '1',
        postUrl: 'https://www.facebook.com/groups/test/posts/1',
        isGeneratedPostId: false,
        content: 'Test post',
        collectedAt: '2026-10-15T00:00:00.000Z',
    }];
    const collect = mock.method(facebookService, 'collectPosts', async () => posts);
    const response = await fetch(`${baseUrl}/api/facebook/posts`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { total: 1, posts });
    assert.equal(collect.mock.callCount(), 1);
});

test('Collection failure returns JSON 500 instead of an empty success', async () => {
    mock.method(facebookService, 'collectPosts', async () => {
        throw new Error('Facebook session unavailable');
    });
    mock.method(console, 'error', () => {});
    const response = await fetch(`${baseUrl}/api/facebook/posts`);
    assert.equal(response.status, 500);
    assert.deepEqual(await response.json(), {
        error: 'FACEBOOK_COLLECT_FAILED',
        message: 'Facebook session unavailable',
    });
});

test('Service closes the browser if creating the context fails', async () => {
    let closeCalls = 0;
    mock.method(console, 'log', () => {});
    mock.method(chromium, 'launch', async () => ({
        newContext: async () => { throw new Error('Invalid storage state'); },
        close: async () => { closeCalls++; },
    }));
    await assert.rejects(facebookService.collectPosts(), /Invalid storage state/);
    assert.equal(closeCalls, 1);
});
