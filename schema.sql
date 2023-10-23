\c mydatabase;

CREATE TABLE pessoaentity (
    id uuid NOT NULL,
    apelido character varying(255),
    nascimento character varying(255),
    nome character varying(255),
    stack character varying(255),
    text TEXT
);


ALTER TABLE pessoaentity OWNER TO sarah;

ALTER TABLE ONLY pessoaentity
    ADD CONSTRAINT pessoaentity_pkey PRIMARY KEY (id);

ALTER TABLE ONLY pessoaentity
    ADD CONSTRAINT uk1upmjfam63kbek6e9kui0wil3 UNIQUE (apelido);

CREATE UNLOGGED TABLE IF NOT EXISTS cache (
        key TEXT CONSTRAINT CACHE_ID_PK PRIMARY KEY,
        data jsonb
    );

CREATE UNIQUE INDEX IF NOT EXISTS IDX_ID_UUID ON pessoaentity (id);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS IDX_PESSOAS_TEXT_TGRM ON pessoaentity 
USING GIST (text GIST_TRGM_OPS(SIGLEN=64));

