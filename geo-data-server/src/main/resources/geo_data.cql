
-- Create geo_data keySpace

-- CREATE KEYSPACE IF NOT EXISTS  geo_data WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1};

-- for multi datacenters deployments
CREATE KEYSPACE IF NOT EXISTS geo_data WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'dc1' : 1, 'dc2' : 1 };

USE geo_data;

CREATE TYPE geo_data.coordinate (
    lat double,
    lon double,
);

CREATE TYPE geo_data.segment (
    start frozen<geo_data.coordinate>,
    end frozen<geo_data.coordinate>,
);

-- Goal : Save substations data
CREATE TABLE substations (country text, substationID text,coordinate frozen<geo_data.coordinate>, voltages frozen<list<int>>, PRIMARY KEY (country, substationID));



-- Goal : Save  lines
CREATE TABLE lines (country text, voltage int, lineID text, coordinates frozen<list<geo_data.coordinate>>
, aerial Boolean, ordered Boolean, PRIMARY KEY (country, voltage, lineID));

-- Rule 1 : Spread data around the cluster OK
-- Rule 2 : Minimize the number of partition read (one read per query)



