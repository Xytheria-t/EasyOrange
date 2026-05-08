import { useEffect, useRef, useState } from 'react'

interface UserStory {
  id: number
  avatar: string
  name: string
  role: string
  school: string
  story: string
  highlight: string
  category: string
  image: string
}

const USER_STORIES: UserStory[] = [
  {
    id: 1,
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&h=150&fit=crop',
    name: '小雨',
    role: '大三学生',
    school: '清华大学',
    story: '考研结束后，把用过的复习资料和教材都在这里出售，不仅回血了2000多块，还帮助了学弟学妹，一举两得！',
    highlight: '成功交易 28 笔',
    category: '教材资料',
    image: 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600&auto=format&fit=crop'
  },
  {
    id: 2,
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop',
    name: '阿杰',
    role: '研一学生',
    school: '北京大学',
    story: '在这里买了一台二手相机，成色比描述的还好！卖家很靠谱，还教我怎么使用，省下了好几千块钱。',
    highlight: '节省 ¥3,500',
    category: '电子数码',
    image: 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&auto=format&fit=crop'
  },
  {
    id: 3,
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&h=150&fit=crop',
    name: '小美',
    role: '大二学生',
    school: '复旦大学',
    story: '毕业季买了很多生活用品和装饰品，价格超级实惠，还认识了很多有趣的学长学姐，收获满满！',
    highlight: '发现宝藏好物',
    category: '生活用品',
    image: 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?w=600&auto=format&fit=crop'
  },
  {
    id: 4,
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&h=150&fit=crop',
    name: '大伟',
    role: '大四学生',
    school: '浙江大学',
    story: '把自己不用的自行车和健身器材都卖掉了，不仅清理了宿舍空间，还赚了一笔生活费，平台担保交易很放心。',
    highlight: '快速出手闲置',
    category: '运动健身',
    image: 'https://images.unsplash.com/photo-1571068316344-75bc76f77890?w=600&auto=format&fit=crop'
  }
]

export default function UserStoriesSection() {
  const [activeStory, setActiveStory] = useState(0)
  const [isVisible, setIsVisible] = useState(false)
  const sectionRef = useRef<HTMLElement>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          setIsVisible(true)
        }
      },
      { threshold: 0.15 }
    )

    if (sectionRef.current) {
      observer.observe(sectionRef.current)
    }

    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    timerRef.current = setInterval(() => {
      setActiveStory((prev) => (prev + 1) % USER_STORIES.length)
    }, 5000)

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
      }
    }
  }, [])

  const currentStory = USER_STORIES[activeStory]

  return (
    <section ref={sectionRef} className="user-stories-section">
      <div className="stories-bg">
        <div className="stories-gradient-orb orb-1" />
        <div className="stories-gradient-orb orb-2" />
        <div className="stories-mesh" />
      </div>

      <div className="container">
        <div className={`section-header reveal ${isVisible ? 'revealed' : ''}`}>
          <span className="section-tag">真实故事</span>
          <h2 className="section-title">他们在 EasyOrange 的经历</h2>
          <p className="section-desc">来自全国各地高校学生的真实分享</p>
        </div>

        <div className={`stories-container ${isVisible ? 'visible' : ''}`}>
          <div className="story-main">
            <div className="story-card glass-card">
              <div className="story-image-wrapper">
                <img
                  src={currentStory.image}
                  alt={currentStory.category}
                  className="story-image"
                />
                <div className="story-image-overlay" />
                <div className="story-category-badge">
                  {currentStory.category}
                </div>
              </div>

              <div className="story-content">
                <div className="story-user">
                  <img
                    src={currentStory.avatar}
                    alt={currentStory.name}
                    className="story-avatar"
                  />
                  <div className="story-user-info">
                    <h4 className="story-name">{currentStory.name}</h4>
                    <p className="story-role">{currentStory.role} · {currentStory.school}</p>
                  </div>
                </div>

                <blockquote className="story-text">
                  "{currentStory.story}"
                </blockquote>

                <div className="story-highlight">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                  <span>{currentStory.highlight}</span>
                </div>
              </div>
            </div>
          </div>

          <div className="story-indicators">
            {USER_STORIES.map((story, index) => (
              <button
                key={story.id}
                className={`indicator ${index === activeStory ? 'active' : ''}`}
                onClick={() => setActiveStory(index)}
                aria-label={`查看 ${story.name} 的故事`}
              >
                <img src={story.avatar} alt={story.name} />
                {index === activeStory && (
                  <div className="indicator-progress" />
                )}
              </button>
            ))}
          </div>

          <div className="story-thumbnails">
            {USER_STORIES.map((story, index) => (
              <div
                key={story.id}
                className={`thumbnail-card ${index === activeStory ? 'active' : ''}`}
                onClick={() => setActiveStory(index)}
              >
                <img src={story.image} alt={story.category} />
                <div className="thumbnail-overlay">
                  <span className="thumbnail-category">{story.category}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className={`stories-stats ${isVisible ? 'visible' : ''}`}>
          <div className="stat-card">
            <div className="stat-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
            <div className="stat-info">
              <span className="stat-value">50,000+</span>
              <span className="stat-label">活跃用户</span>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
              </svg>
            </div>
            <div className="stat-info">
              <span className="stat-value">200+</span>
              <span className="stat-label">覆盖高校</span>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
            </div>
            <div className="stat-info">
              <span className="stat-value">平均 2 小时</span>
              <span className="stat-label">成交速度</span>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
              </svg>
            </div>
            <div className="stat-info">
              <span className="stat-value">4.9/5.0</span>
              <span className="stat-label">用户评分</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
