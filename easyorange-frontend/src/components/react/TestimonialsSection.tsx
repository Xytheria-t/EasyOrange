export default function TestimonialsSection() {
  const testimonials = [
    {
      id: 1,
      name: '李明',
      school: '北京大学',
      avatar: '👨‍🎓',
      content: '作为大四毕业生，在这里卖掉了所有带不走的书籍和生活用品，真的太方便了！买家都是本校同学，交易很放心。',
      rating: 5,
      tags: ['卖家', '毕业生'],
    },
    {
      id: 2,
      name: '张雨',
      school: '清华大学',
      avatar: '👩‍🎓',
      content: '用 EasyOrange 淘到了很多便宜的教材和考研资料，比买新的省了好多钱。平台还有验货保障，非常靠谱！',
      rating: 5,
      tags: ['买家', '考研党'],
    },
    {
      id: 3,
      name: '王浩然',
      school: '复旦大学',
      avatar: '🧑‍💻',
      content: '发布商品特别简单，拍照上传后很快就有人咨询。已经成功卖出好几件闲置物品了，强烈推荐给同学们！',
      rating: 5,
      tags: ['卖家', '数码达人'],
    },
    {
      id: 4,
      name: '陈思雨',
      school: '上海交通大学',
      avatar: '👩‍🎨',
      content: '界面设计很好看，用起来很流畅。最喜欢的是可以按学校筛选，这样交易起来更方便，都是校友信得过。',
      rating: 5,
      tags: ['买家', '设计控'],
    },
  ]

  return (
    <section className="testimonials-section">
      <div className="container">
        <div className="section-header">
          <span className="section-tag">用户评价</span>
          <h2 className="section-title">听听他们怎么说</h2>
          <p className="section-desc">来自全国高校学子的真实反馈</p>
        </div>

        <div className="testimonials-grid">
          {testimonials.map((testimonial) => (
            <div
              key={testimonial.id}
              className="testimonial-card glass-card"
            >
              <div className="testimonial-header">
                <div className="testimonial-avatar">{testimonial.avatar}</div>
                <div className="testimonial-info">
                  <h4 className="testimonial-name">{testimonial.name}</h4>
                  <p className="testimonial-school">{testimonial.school}</p>
                </div>
                <div className="testimonial-rating">
                  {[...Array(testimonial.rating)].map((_, i) => (
                    <svg
                      key={i}
                      className="star-icon"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                    >
                      <path d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
                    </svg>
                  ))}
                </div>
              </div>

              <p className="testimonial-content">"{testimonial.content}"</p>

              <div className="testimonial-tags">
                {testimonial.tags.map((tag) => (
                  <span key={tag} className="tag-badge">{tag}</span>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="text-center">
          <a
            href="/testimonials"
            className="btn btn-primary btn-lg"
          >
            <span>查看更多评价</span>
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </a>
        </div>
      </div>
    </section>
  )
}
