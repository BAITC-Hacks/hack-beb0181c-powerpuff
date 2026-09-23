CREATE INDEX idx_products_article_lower ON products (lower(article));
CREATE INDEX idx_products_supplier_article_lower ON products (lower(supplier_article));
