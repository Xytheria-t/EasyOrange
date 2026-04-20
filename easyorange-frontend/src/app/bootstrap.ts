import header from '../components/Header.js';
import { routes, type RouteName } from './routeConfig.js';
import { navigation } from './navigation.js';

export interface PageModule {
    init(): Promise<void> | void;
    destroy?(): void;
}

export interface BootstrapOptions {
    routeName: RouteName;
    page: PageModule;
}

export async function bootstrapPage(options: BootstrapOptions): Promise<void> {
    const route = routes[options.routeName];

    document.title = `${route.title} - EasyOrange`;

    if (route.requiresAuth && !navigation.requireAuth()) {
        return;
    }

    header.init();
    if (route.navKey) {
        header.setActiveNav(route.navKey);
    }

    await options.page.init();

    window.addEventListener('beforeunload', () => {
        options.page.destroy?.();
        header.destroy();
    });
}
