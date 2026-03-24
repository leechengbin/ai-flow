-- V2__add_bidding_check_fields.sql

-- Add columns to clauses table
ALTER TABLE clauses ADD COLUMN title VARCHAR(255);
ALTER TABLE clauses ADD COLUMN type VARCHAR(20);
ALTER TABLE clauses ADD COLUMN start_page INT;
ALTER TABLE clauses ADD COLUMN end_page INT;

-- Create clause embeddings table for semantic matching
CREATE TABLE clause_embeddings (
    id VARCHAR(50) PRIMARY KEY,
    clause_id VARCHAR(50),
    document_id VARCHAR(50),
    content_vector TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for clause_embeddings
CREATE INDEX idx_embeddings_document ON clause_embeddings(document_id);
CREATE INDEX idx_embeddings_clause ON clause_embeddings(clause_id);

-- Create bidding_check_records table for report persistence
CREATE TABLE bidding_check_records (
    id VARCHAR(50) PRIMARY KEY,
    tender_document_id VARCHAR(50),
    bidding_document_id VARCHAR(50),
    report_json TEXT,
    total_score DECIMAL(5,2),
    coverage_rate DECIMAL(5,2),
    format_score DECIMAL(5,2),
    risk_level VARCHAR(20),
    elimination_risk BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for bidding_check_records
CREATE INDEX idx_check_records_tender ON bidding_check_records(tender_document_id);
CREATE INDEX idx_check_records_bidding ON bidding_check_records(bidding_document_id);
CREATE INDEX idx_check_records_risk ON bidding_check_records(risk_level);

-- Add foreign key constraints
ALTER TABLE clause_embeddings ADD CONSTRAINT fk_embeddings_clause
    FOREIGN KEY (clause_id) REFERENCES clauses(id) ON DELETE CASCADE;
ALTER TABLE clause_embeddings ADD CONSTRAINT fk_embeddings_document
    FOREIGN KEY (document_id) REFERENCES bidding_documents(id) ON DELETE CASCADE;
ALTER TABLE bidding_check_records ADD CONSTRAINT fk_check_records_tender
    FOREIGN KEY (tender_document_id) REFERENCES bidding_documents(id);
ALTER TABLE bidding_check_records ADD CONSTRAINT fk_check_records_bidding
    FOREIGN KEY (bidding_document_id) REFERENCES bidding_documents(id);
