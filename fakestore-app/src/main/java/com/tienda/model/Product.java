package com.tienda.model;

import java.util.Map;

/** Modelo que empata exactamente con la estructura JSON de Fake Store API. */
public record Product(int id, String title, double price, String description,
                      String category, String image, Rating rating) {

    @SuppressWarnings("unchecked")
    public static Product fromJson(Map<String, Object> m) {
        Rating r = null;
        if (m.get("rating") instanceof Map<?, ?> rm) {
            Map<String, Object> rating = (Map<String, Object>) rm;
            r = new Rating(num(rating.get("rate")).doubleValue(), num(rating.get("count")).intValue());
        }
        return new Product(
                num(m.get("id")).intValue(),
                str(m.get("title")),
                num(m.get("price")).doubleValue(),
                str(m.get("description")),
                str(m.get("category")),
                str(m.get("image")),
                r);
    }

    private static Number num(Object o) { return o instanceof Number n ? n : 0; }
    private static String str(Object o) { return o == null ? "" : o.toString(); }
}
