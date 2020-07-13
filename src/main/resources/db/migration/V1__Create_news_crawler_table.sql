create table LINKS_TO_BE_PROCESSED(link varchar (2100));


create table LINKS_ALREADY_PROCESSED(link varchar (1000));

create table NEWS(
id bigint primary key auto_increment,
title text,
content text,
url varchar(1000),
created_at timestamp,
modified_at timestamp) DEFAULT CHARSET=utf8mb4;