import { lazy, Suspense } from 'react'
import HeroSection from '@/components/sections/HeroSection'

const CategoriesSection = lazy(() => import('@/components/sections/CategoriesSection'))
const ProductsSection = lazy(() => import('@/components/sections/ProductsSection'))
const ServicesSection = lazy(() => import('@/components/sections/ServicesSection'))
const UserStoriesSection = lazy(() => import('@/components/sections/UserStoriesSection'))
const AchievementsSection = lazy(() => import('@/components/sections/AchievementsSection'))
const ReviewsSection = lazy(() => import('@/components/sections/ReviewsSection'))

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
)

function HomePage() {
  return (
    <>
      <HeroSection />

      <Suspense fallback={<SectionSkeleton />}>
        <CategoriesSection />
      </Suspense>

      <Suspense fallback={<SectionSkeleton />}>
        <ProductsSection />
      </Suspense>

      <Suspense fallback={<SectionSkeleton />}>
        <UserStoriesSection />
      </Suspense>

      <Suspense fallback={<SectionSkeleton />}>
        <ServicesSection />
      </Suspense>

      <Suspense fallback={<SectionSkeleton />}>
        <AchievementsSection />
      </Suspense>

      <Suspense fallback={<SectionSkeleton />}>
        <ReviewsSection />
      </Suspense>
    </>
  )
}

export default HomePage
