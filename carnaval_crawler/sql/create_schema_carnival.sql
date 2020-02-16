create table carnival_address(id_address serial,
        desc_address varchar(200),
        coords geometry,
        primary key(id_address));

create index idx_carnival_address_desc_address on
carnival_address(desc_address);

create table profile(id_profile serial,
        desc_profile varchar(20),
        primary key(id_profile));

create index idx_profile_desc on
profile(desc_profile);

create table music_type(id_music_type serial,
        desc_music_type varchar(20),
        primary key(id_music_type));

create index idx_music_type_desc on
music_type(desc_music_type);

create table carnival_block(id_block serial,
        "name" varchar(90),
        description text,
        "date" timestamp,
        start_address_id int,
        final_address_id int,
        primary key(id_block),
        foreign key (start_address_id) references carnival_address(id_address),
        foreign key (final_address_id) references carnival_address(id_address));

create table music_typexcarnival_block(id_music_typexcarnival_block serial,
        id_block int,
        id_music_type int,
        primary key(id_music_typexcarnival_block),
        foreign key (id_block) references carnival_block(id_block),
        foreign key (id_music_type) references music_type(id_music_type));

create table profilexcarnival_block(id_profilexcarnival_block serial,
        id_block int,
        id_profile int,
        primary key(id_profilexcarnival_block),
        foreign key (id_block) references carnival_block(id_block),
        foreign key (id_profile) references profile(id_profile));

create table carnival_block_route(id_carnival_block_route serial,
        id_block int,
        id_address int,
        "order" int,
        primary key(id_carnival_block_route),
        foreign key (id_block) references carnival_block(id_block),
        foreign key (id_address) references carnival_address(id_address));

