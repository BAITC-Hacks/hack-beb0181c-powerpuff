ALTER TABLE customer_sessions ADD COLUMN cart_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cart_items ADD COLUMN article TEXT;
ALTER TABLE cart_items ADD COLUMN image TEXT;
ALTER TABLE cart_items ADD COLUMN order_multiple TEXT;
ALTER TABLE cart_proposals ADD COLUMN operation VARCHAR(10) NOT NULL DEFAULT 'ADD';
ALTER TABLE cart_proposals ADD COLUMN cart_version BIGINT NOT NULL DEFAULT 0;
-- Pre-upgrade proposals have no trustworthy base version. Require a new explicit proposal.
UPDATE cart_proposals SET status='SUPERSEDED' WHERE status='PENDING';
CREATE TABLE cart_mutations (
    session_id UUID NOT NULL REFERENCES customer_sessions(id) ON DELETE CASCADE,
    request_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity NUMERIC(38,10),
    cart_version BIGINT NOT NULL,
    proposal_id UUID REFERENCES cart_proposals(id),
    PRIMARY KEY (session_id, request_id)
);
