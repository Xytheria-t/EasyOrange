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

assertSection('page title and meta', () => {
    assert.match(html, /<title>EasyOrange - 校园二手交易平台<\/title>/);
    assert.match(html, /校园二手交易平台/);
    assert.match(html, /让闲置流转/);
    assert.match(html, /让价值延续/);
    assert.match(html, /meta.*robots.*index.*follow/s);
});

assertSection('static resources', () => {
    assert.match(html, /<link rel="stylesheet" href="\/src\/styles\/main\.css">/);
    assert.match(html, /<script src="\/src\/main\.tsx" type="module"><\/script>/);
});

assertSection('root element', () => {
    assert.match(html, /<div id="root"><\/div>/);
});
