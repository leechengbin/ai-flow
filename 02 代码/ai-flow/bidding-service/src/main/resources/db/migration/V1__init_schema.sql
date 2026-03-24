-- V1__init_schema.sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE bidding_documents (
    id VARCHAR(50) PRIMARY KEY, title VARCHAR(255) NOT NULL, project_id VARCHAR(50),
    uploader_id VARCHAR(50) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    current_version VARCHAR(10), tender_requirements TEXT, tender_file_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clauses (
    id VARCHAR(50) PRIMARY KEY, document_id VARCHAR(50) NOT NULL REFERENCES bidding_documents(id),
    clause_number VARCHAR(20), content TEXT NOT NULL, is_starred BOOLEAN DEFAULT FALSE,
    clause_type VARCHAR(20), response_status VARCHAR(20), page_number INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clause_issues (
    id VARCHAR(50) PRIMARY KEY, clause_id VARCHAR(50) REFERENCES clauses(id), clause_number VARCHAR(20),
    issue_type VARCHAR(20) NOT NULL, original_text TEXT, requirement_text TEXT, suggestion_text TEXT,
    severity VARCHAR(20) NOT NULL, elimination_risk BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bidding_cases (
    id VARCHAR(50) PRIMARY KEY, tender_title VARCHAR(255) NOT NULL, industry VARCHAR(50),
    region VARCHAR(50), winning_bidder VARCHAR(255), bid_amount DECIMAL(15,2), winning_date DATE,
    tender_file_path VARCHAR(500), embedding VECTOR(1536), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_versions (
    id VARCHAR(50) PRIMARY KEY, document_id VARCHAR(50) NOT NULL REFERENCES bidding_documents(id),
    version_number VARCHAR(10) NOT NULL, file_path VARCHAR(500) NOT NULL, diff_from_previous TEXT,
    created_by VARCHAR(50), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE review_processes (
    id VARCHAR(50) PRIMARY KEY, document_id VARCHAR(50) NOT NULL REFERENCES bidding_documents(id),
    current_state VARCHAR(30) NOT NULL, submitter_id VARCHAR(50) NOT NULL, reviewer_id VARCHAR(50),
    deadline TIMESTAMP, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE state_transitions (
    id VARCHAR(50) PRIMARY KEY, review_id VARCHAR(50) NOT NULL REFERENCES review_processes(id),
    from_state VARCHAR(30), to_state VARCHAR(30) NOT NULL, actor VARCHAR(50) NOT NULL,
    action VARCHAR(100), comment TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE suggestions (
    id VARCHAR(50) PRIMARY KEY, issue_id VARCHAR(50) REFERENCES clause_issues(id),
    document_id VARCHAR(50) NOT NULL REFERENCES bidding_documents(id),
    granularity VARCHAR(20) NOT NULL, original_content TEXT, suggested_content TEXT, explanation TEXT,
    status VARCHAR(20) DEFAULT 'PENDING', generated_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clauses_document_id ON clauses(document_id);
CREATE INDEX idx_clause_issues_clause_id ON clause_issues(clause_id);
CREATE INDEX idx_bidding_cases_industry ON bidding_cases(industry);
CREATE INDEX idx_review_processes_document_id ON review_processes(document_id);
CREATE INDEX idx_state_transitions_review_id ON state_transitions(review_id);
CREATE INDEX idx_suggestions_document_id ON suggestions(document_id);