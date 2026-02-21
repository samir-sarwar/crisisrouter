-- Enable PostGIS extension BEFORE Hibernate creates tables
-- This must run before ddl-auto=update so geometry columns can be created
CREATE EXTENSION IF NOT EXISTS postgis;
