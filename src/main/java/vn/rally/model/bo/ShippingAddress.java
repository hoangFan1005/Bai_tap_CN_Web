package vn.rally.model.bo;

record ShippingAddress(String recipientName, String phone, String address) {
    static ShippingAddress validate(String rawName, String rawPhone, String rawAddress) throws CommerceException {
        String name = clean(rawName);
        String phone = clean(rawPhone);
        String address = clean(rawAddress);
        if (name.isEmpty() || name.length() > 120) {
            throw new CommerceException("Vui lòng nhập tên người nhận (tối đa 120 ký tự).");
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (!phone.matches("\\+?[0-9][0-9 .()-]*") || phone.length() > 24
                || digits.length() < 8 || digits.length() > 15) {
            throw new CommerceException("Vui lòng nhập số điện thoại hợp lệ.");
        }
        if (address.isEmpty() || address.length() > 350) {
            throw new CommerceException("Vui lòng nhập địa chỉ giao hàng (tối đa 350 ký tự).");
        }
        return new ShippingAddress(name, phone, address);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
