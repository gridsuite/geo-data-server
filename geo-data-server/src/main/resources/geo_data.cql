CREATE TYPE coordinate (
    lat double,
    lon double,
);

CREATE TABLE substations (
    country text,
    id text,
    coordinate frozen<coordinate>,
    PRIMARY KEY (country, id)
);

CREATE TABLE lines (
    country text,
    id text,
    side1 boolean,
    otherCountry text,
    substationStart text,
    substationEnd text,
    coordinates frozen<list<coordinate>>,
    PRIMARY KEY (country, id)
);
