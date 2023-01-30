drop table if exists telegram_user;

create table telegram_user
(
    id            bigint  not null unique auto_increment,
    telegram_id   bigint  not null,
    user_id       bigint  not null unique,
    refresh_token varchar,
    command_state varchar not null,
    active        boolean not null,

    primary key (id)
);
