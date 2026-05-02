import HeroSection from '@/components/sections/HeroSection'
import CategoriesSection from '@/components/sections/CategoriesSection'
import ProductsSection from '@/components/sections/ProductsSection'
import FeaturedSection from '@/components/sections/FeaturedSection'
import ServicesSection from '@/components/sections/ServicesSection'
import { useScrollReveal } from '@/hooks'

function HomePage() {
  useScrollReveal()

  return (
    <>
      <HeroSection />

      {/* 分类导航 */}
      <CategoriesSection />

      {/* 热门商品 */}
      <ProductsSection />

      {/* 服务保障 */}
      <ServicesSection />

      {/* 平台优势 */}
      <FeaturedSection />
    </>
  )
}

export default HomePage