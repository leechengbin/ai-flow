-- V1__init_schema.sql
-- User Service Database Schema

-- Users table
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    full_name VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Roles table
CREATE TABLE roles (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Permissions table
CREATE TABLE permissions (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User-Role join table
CREATE TABLE user_roles (
    user_id VARCHAR(50) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id VARCHAR(50) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Role-Permission join table
CREATE TABLE role_permissions (
    role_id VARCHAR(50) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id VARCHAR(50) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Organizations table
CREATE TABLE organizations (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    org_type VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    parent_id VARCHAR(50) REFERENCES organizations(id),
    level INTEGER NOT NULL DEFAULT 1,
    manager_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User-Organization join table
CREATE TABLE user_organizations (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id VARCHAR(50) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role_in_org VARCHAR(50),
    user_title VARCHAR(100),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_roles_code ON roles(code);
CREATE INDEX idx_permissions_code ON permissions(code);
CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX idx_organizations_code ON organizations(code);
CREATE INDEX idx_organizations_parent_id ON organizations(parent_id);
CREATE INDEX idx_user_organizations_user_id ON user_organizations(user_id);
CREATE INDEX idx_user_organizations_org_id ON user_organizations(organization_id);

-- Insert default roles
INSERT INTO roles (id, code, name, description) VALUES
    ('role-admin', 'ADMIN', 'System Administrator', 'Full system access'),
    ('role-user', 'USER', 'Regular User', 'Basic user access'),
    ('role-manager', 'MANAGER', 'Manager', 'Management level access');

-- Insert default permissions for user resource
INSERT INTO permissions (id, code, name, description, resource, action) VALUES
    ('perm-user-create', 'user:create', 'Create User', 'Permission to create new users', 'user', 'CREATE'),
    ('perm-user-read', 'user:read', 'Read User', 'Permission to read user information', 'user', 'READ'),
    ('perm-user-update', 'user:update', 'Update User', 'Permission to update user information', 'user', 'UPDATE'),
    ('perm-user-delete', 'user:delete', 'Delete User', 'Permission to delete users', 'user', 'DELETE');

-- Insert default permissions for role resource
INSERT INTO permissions (id, code, name, description, resource, action) VALUES
    ('perm-role-create', 'role:create', 'Create Role', 'Permission to create new roles', 'role', 'CREATE'),
    ('perm-role-read', 'role:read', 'Read Role', 'Permission to read role information', 'role', 'READ'),
    ('perm-role-update', 'role:update', 'Update Role', 'Permission to update role information', 'role', 'UPDATE'),
    ('perm-role-delete', 'role:delete', 'Delete Role', 'Permission to delete roles', 'role', 'DELETE');

-- Insert default permissions for organization resource
INSERT INTO permissions (id, code, name, description, resource, action) VALUES
    ('perm-org-create', 'org:create', 'Create Organization', 'Permission to create organizations', 'organization', 'CREATE'),
    ('perm-org-read', 'org:read', 'Read Organization', 'Permission to read organization information', 'organization', 'READ'),
    ('perm-org-update', 'org:update', 'Update Organization', 'Permission to update organization information', 'organization', 'UPDATE'),
    ('perm-org-delete', 'org:delete', 'Delete Organization', 'Permission to delete organizations', 'organization', 'DELETE');

-- Assign all permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-admin', 'perm-user-create'),
    ('role-admin', 'perm-user-read'),
    ('role-admin', 'perm-user-update'),
    ('role-admin', 'perm-user-delete'),
    ('role-admin', 'perm-role-create'),
    ('role-admin', 'perm-role-read'),
    ('role-admin', 'perm-role-update'),
    ('role-admin', 'perm-role-delete'),
    ('role-admin', 'perm-org-create'),
    ('role-admin', 'perm-org-read'),
    ('role-admin', 'perm-org-update'),
    ('role-admin', 'perm-org-delete');

-- Assign basic permissions to USER role
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-user', 'perm-user-read'),
    ('role-user', 'perm-org-read');

-- Assign management permissions to MANAGER role
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-manager', 'perm-user-read'),
    ('role-manager', 'perm-user-update'),
    ('role-manager', 'perm-role-read'),
    ('role-manager', 'perm-org-read'),
    ('role-manager', 'perm-org-update');