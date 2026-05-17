#!/usr/bin/env node
import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const vitePath = join(__dirname, 'node_modules/vite/bin/vite.js');

const child = spawn('node', [vitePath, 'dev'], {
    stdio: ['inherit', 'pipe', 'pipe'],
    cwd: __dirname,
    env: { ...process.env }
});

child.stdout.on('data', (data) => {
    process.stdout.write(data);
});

child.stderr.on('data', (data) => {
    const message = data.toString();
    if (!message.includes('DEP0205') && !message.includes('module.register()')) {
        process.stderr.write(message);
    }
});

child.on('close', (code) => {
    process.exit(code ?? 0);
});
