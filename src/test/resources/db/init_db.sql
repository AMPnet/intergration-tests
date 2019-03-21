CREATE DATABASE crowdfunding ENCODING 'UTF-8';
CREATE USER crowdfunding WITH PASSWORD 'password';

CREATE DATABASE blockchain ENCODING 'UTF-8';
CREATE USER blockchain WITH PASSWORD 'password';

CREATE ROLE cf_role;
GRANT cf_role TO crowdfunding;
GRANT cf_role TO blockchain;
