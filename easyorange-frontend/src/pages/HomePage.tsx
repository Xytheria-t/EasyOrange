import HeroSection from '@/components/sections/HeroSection'
import CategoriesSection from '@/components/sections/CategoriesSection'
import ProductsSection from '@/components/sections/ProductsSection'
import FeaturedSection from '@/components/sections/FeaturedSection'
import ServicesSection from '@/components/sections/ServicesSection'
import TestimonialsSection from '@/components/sections/TestimonialsSection'
import BannerSection from '@/components/sections/BannerSection'

function HomePage() {
  return (
    <>
      <HeroSection />

      {/* 分类导航 */}
      <CategoriesSection />

      {/* 热门商品 */}
      <ProductsSection />

      {/* 平台优势 */}
      <FeaturedSection />

      {/* 广告横幅 */}
      <BannerSection />

      {/* 服务保障 */}
      <ServicesSection />

      {/* 用户评价 */}
      <TestimonialsSection />
    </>
  )
}

export default HomePage