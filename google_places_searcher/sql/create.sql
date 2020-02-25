create table place_type(id_place_type serial,
desc_type varchar(30),
primary key(id_place_type));

create index idx_place_type_desc_type on
place_type(desc_type);

create table place(id_place varchar(40),
coords geometry,
primary key(id_place));

create table placexcarnival_address(id_place varchar(40),
id_address int,
primary key(id_place, id_address),
foreign key (id_place) references place(id_place),
foreign key (id_address) references carnival_address(id_address));
