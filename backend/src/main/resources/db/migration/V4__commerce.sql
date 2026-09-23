CREATE TABLE customer_sessions (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE cart_items (
    session_id UUID NOT NULL REFERENCES customer_sessions(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    name TEXT NOT NULL,
    quantity NUMERIC(38,10) NOT NULL CHECK (quantity>0),
    price NUMERIC(38,10) NOT NULL CHECK (price>=0),
    PRIMARY KEY(session_id,product_id)
);
CREATE TABLE cart_proposals (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES customer_sessions(id) ON DELETE CASCADE,
    request_id UUID NOT NULL,
    product_id BIGINT NOT NULL,
    name TEXT NOT NULL,
    quantity NUMERIC(38,10) NOT NULL CHECK (quantity>0),
    price NUMERIC(38,10) NOT NULL,
    stock NUMERIC(38,10) NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(session_id,request_id)
);
CREATE TABLE chat_messages (
    session_id UUID NOT NULL REFERENCES customer_sessions(id) ON DELETE CASCADE,
    request_id UUID NOT NULL,
    user_text TEXT NOT NULL,
    response JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(session_id,request_id)
);
