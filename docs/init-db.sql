-- Optional: Additional database initialization commands
-- This file is executed when PostgreSQL container starts

-- Create UUID extension if not already created
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create search path
SET search_path TO public;

-- Log initialization
SELECT 'Database initialized successfully' as message;
