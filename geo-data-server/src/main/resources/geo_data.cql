
-- Create geo_data keySpace

-- CREATE KEYSPACE IF NOT EXISTS  geo_data WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1};

-- for multi datacenters deployments
CREATE KEYSPACE IF NOT EXISTS geo_data WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'dc1' : 1, 'dc2' : 1 };

CREATE TYPE geo_data.coordinate (
    lat double,
    lon double,
);

CREATE TABLE geo_data.substations (
    country text,
    id text,
    coordinate frozen<geo_data.coordinate>,
    PRIMARY KEY (country, id)
);

CREATE TABLE geo_data.lines (
    country text,
    id text,
    side1 boolean,
    otherCountry text,
    coordinates frozen<list<geo_data.coordinate>>,
    PRIMARY KEY (country, id)
);

-- Rule 1 : Spread data around the cluster OK
-- Rule 2 : Minimize the number of partition read (one read per query)
