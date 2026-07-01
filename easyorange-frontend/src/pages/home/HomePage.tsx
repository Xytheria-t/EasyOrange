import { lazy, Suspense } from 'react';
import HeroSection from '@/components/sections/HeroSection';

const AIFeaturesSection = lazy(() => import('@/components/sections/AIFeaturesSection'));
const CategoriesSection = lazy(() => import('@/components/sections/CategoriesSection'));
const AIRecommendSection = lazy(() => import('@/components/sections/AIRecommendSection'));
const ProductsSection = lazy(() => import('@/components/sections/ProductsSection'));

const SectionSkeleton = () => (
    <div className="section-skeleton">
        <div className="skeleton-content">
            <div className="skeleton-title" />
            <div className="skeleton-grid">
                {[...Array(4)].map((_, i) => (
                    <div key={i} className="skeleton-card">
                        <div className="skeleton-image" />
                        <div className="skeleton-text" />
                    </div>
                ))}
            </div>
        </div>
    </div>
);

function HomePage() {
    return (
        <>
            <HeroSection />

            <Suspense fallback={<SectionSkeleton />}>
                <AIFeaturesSection />
            </Suspense>

            <Suspense fallback={<SectionSkeleton />}>
                <CategoriesSection />
            </Suspense>

            <Suspense fallback={<SectionSkeleton />}>
                <AIRecommendSection />
            </Suspense>

            <Suspense fallback={<SectionSkeleton />}>
                <ProductsSection />
            </Suspense>
        </>
    );
}

export default HomePage;
