import HeroSection from '@/components/sections/HeroSection'
import CategoriesSection from '@/components/sections/CategoriesSection'
import ProductsSection from '@/components/sections/ProductsSection'
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

      {/* 平台保障 */}
      <ServicesSection />
    </>
  )
}

export default HomePage
