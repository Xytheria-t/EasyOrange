package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;

public final class ShippingContact implements ValueObject {

    private final String address;
    private final String phone;

    public ShippingContact(String address, String phone) {
        BizRequire.notBlank(address, "收货地址不能为空");
        BizRequire.notBlank(phone, "联系电话不能为空");
        this.address = address.trim();
        this.phone = phone.trim();
    }

    public String address() {
        return address;
    }

    public String phone() {
        return phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShippingContact that = (ShippingContact) o;
        return Objects.equals(address, that.address) && Objects.equals(phone, that.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, phone);
    }

    @Override
    public String toString() {
        return "ShippingContact{address='" + address + "', phone='" + phone + "'}";
    }
}
