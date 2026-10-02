package com.tienda.service;

import com.tienda.model.Product;
import com.tienda.util.Json;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Cliente HTTP de Fake Store API. Debe llamarse SIEMPRE desde hilos de fondo. */
public class ProductApi {
    private static final String BASE = "https://fakestoreapi.com";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();

    // GET /products
    public List<Product> getProducts() throws ApiException {
        return toProducts(send(request("/products").GET().build()));
    }

    // GET /products/categories
    public List<String> getCategories() throws ApiException {
        Object o = parse(send(request("/products/categories").GET().build()));
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) l.forEach(x -> out.add(String.valueOf(x)));
        return out;
    }

    // GET /products/category/{category}
    public List<Product> getByCategory(String category) throws ApiException {
        String enc = URLEncoder.encode(category, StandardCharsets.UTF_8).replace("+", "%20");
        return toProducts(send(request("/products/category/" + enc).GET().build()));
    }

    // GET /products/{id}
    @SuppressWarnings("unchecked")
    public Product getById(int id) throws ApiException {
        String body = send(request("/products/" + id).GET().build());
        if (body == null || body.isBlank() || body.trim().equals("null"))
            throw new ApiException("El producto no existe");
        Object o = parse(body);
        if (!(o instanceof Map<?, ?> m)) throw new ApiException("El producto no existe");
        return Product.fromJson((Map<String, Object>) m);
    }

    // PUT /products/{id}
    public void update(Product p) throws ApiException {
        String json = "{\"title\":" + Json.quote(p.title())
                + ",\"price\":" + p.price()
                + ",\"description\":" + Json.quote(p.description())
                + ",\"image\":" + Json.quote(p.image())
                + ",\"category\":" + Json.quote(p.category()) + "}";
        send(request("/products/" + p.id())
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json)).build());
    }

    // DELETE /products/{id}
    public void delete(int id) throws ApiException {
        send(request("/products/" + id).DELETE().build());
    }

    // ---- utilidades ----
    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 FakeStoreApp");
    }

    private String send(HttpRequest req) throws ApiException {
        try {
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2)
                throw new ApiException("El servidor respondió con código " + res.statusCode());
            return res.body();
        } catch (IOException e) {
            throw new ApiException("No se pudo conectar con el servidor", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Operación interrumpida", e);
        }
    }

    private Object parse(String body) throws ApiException {
        try { return Json.parse(body); }
        catch (RuntimeException e) { throw new ApiException("Respuesta inválida del servidor", e); }
    }

    @SuppressWarnings("unchecked")
    private List<Product> toProducts(String body) throws ApiException {
        Object o = parse(body);
        List<Product> out = new ArrayList<>();
        if (o instanceof List<?> l) {
            for (Object x : l) out.add(Product.fromJson((Map<String, Object>) x));
        }
        return out;
    }
}
