alter table refresh_token
    add constraint uq_refresh_token_value unique (value);
