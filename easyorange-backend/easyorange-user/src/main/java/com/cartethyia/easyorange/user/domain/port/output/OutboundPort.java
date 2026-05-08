package com.cartethyia.easyorange.user.domain.port.output;

/**
 * 出站端口标记接口
 * <p>
 * 所有出站端口接口都应继承此接口，用于标识：
 * <ul>
 *   <li>领域层与外部基础设施的边界</li>
 *   <li>依赖倒置原则 (DIP) 的抽象接口</li>
 *   <li>六边形架构中的出站适配器契约</li>
 * </ul>
 * 
 * <p>出站端口定义了领域层需要的能力，由基础设施层的适配器实现。
 * 
 * @see <a href="https://alistair.cockburn.us/hexagonal-architecture/">Hexagonal Architecture</a>
 */
public interface OutboundPort {
}
