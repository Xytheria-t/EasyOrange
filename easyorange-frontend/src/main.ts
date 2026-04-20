/**
 * @fileoverview 主入口文件
 * @version 3.0.0
 * @description 精简后的入口文件，使用 HomePage 模块管理首页逻辑
 */

import './styles/main.css';
import './styles/floating-nav.css';
import { network } from './utils/index.js';
import { homePage } from './pages/home/index.js';

homePage.init();
network.checkStatus();

window.addEventListener('beforeunload', () => {
    homePage.destroy();
    network.destroy();
});
