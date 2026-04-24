import { Header } from '@/components/react/Header'
import HeroSection from '@/components/react/HeroSection'
import CategoriesSection from '@/components/react/CategoriesSection'
import ProductsSection from '@/components/react/ProductsSection'
import FeaturedSection from '@/components/react/FeaturedSection'
import ServicesSection from '@/components/react/ServicesSection'
import TestimonialsSection from '@/components/react/TestimonialsSection'
import BannerSection from '@/components/react/BannerSection'
import Footer from '@/components/react/Footer'
import BackgroundEffects from '@/components/react/BackgroundEffects'

function HomePage() {
  return (
    <>
      <BackgroundEffects />
      <Header />
      <main style={{ overflow: 'hidden', paddingTop: '0' }}>
        {/* 首屏区域 */}
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
      </main>
      <Footer />
    </>
  )
}

export default HomePage
