create table test
(
    id      int          not null
        primary key,
    value   varchar(255) null,
    version int          null,
    age     int          null
);

INSERT INTO sqltool.test (id, value, version, age) VALUES (1, 'New value 2', 3, 2);
