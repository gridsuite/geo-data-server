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
    substationStart text,
    substationEnd text,
    coordinates frozen<list<geo_data.coordinate>>,
    PRIMARY KEY (country, id)
);
