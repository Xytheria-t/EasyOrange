import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const html = readFileSync(resolve(process.cwd(), 'index.html'), 'utf8');

function assertSection(name, checks) {
    try {
        checks();
    } catch (error) {
        error.message = `[${name}] ${error.message}`;
        throw error;
    }
}

assertSection('hero copy', () => {
    assert.match(html, /校园专属二手交易平台/);
    assert.match(html, /让闲置流转/);
    assert.match(html, /让价值延续/);
});

assertSection('floating chips and preview card', () => {
    assert.match(html, /hero-floating-chip/);
    assert.match(html, /教材资料/);
    assert.match(html, /电子产品/);
    assert.match(html, /交通工具/);
    assert.match(html, /hero-preview-card/);
});

assertSection('header actions', () => {
    assert.match(html, /首页/);
    assert.match(html, /商品/);
    assert.match(html, /发布/);
    assert.match(html, /admin/i);
});
