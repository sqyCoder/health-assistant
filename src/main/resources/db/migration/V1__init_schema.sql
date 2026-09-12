-- ============================================================
-- V1 初始化全量业务表结构（PostgreSQL 15 / Supabase）
-- ============================================================

CREATE TABLE sys_user (
    id            bigint       NOT NULL,
    username      varchar(50)  NOT NULL,
    password      varchar(100) NOT NULL,
    nickname      varchar(50)  NOT NULL,
    phone         varchar(30),
    email         varchar(100),
    avatar_url    varchar(500),
    role          varchar(20)  NOT NULL DEFAULT 'USER',
    status        smallint     NOT NULL DEFAULT 1,
    last_login_at timestamp,
    last_login_ip varchar(45),
    created_at    timestamp  NOT NULL DEFAULT now(),
    updated_at    timestamp  NOT NULL DEFAULT now(),
    deleted       smallint     NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_user PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_user_username ON sys_user (username);
CREATE INDEX idx_user_phone ON sys_user (phone);
CREATE INDEX idx_user_email ON sys_user (email);

CREATE TABLE sys_user_token (
    id          bigint       NOT NULL,
    user_id     bigint       NOT NULL,
    token_hash  varchar(128) NOT NULL,
    expires_at  timestamp  NOT NULL,
    revoked     smallint     NOT NULL DEFAULT 0,
    client_info varchar(200),
    created_at  timestamp  NOT NULL DEFAULT now(),
    CONSTRAINT pk_sys_user_token PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_token_hash ON sys_user_token (token_hash);
CREATE INDEX idx_token_user ON sys_user_token (user_id);

CREATE TABLE family_member (
    id             bigint       NOT NULL,
    user_id        bigint       NOT NULL,
    name           varchar(50)  NOT NULL,
    relation       varchar(20)  NOT NULL,
    gender         smallint     NOT NULL,
    birth_date     date,
    height_cm      numeric(5,1),
    weight_kg      numeric(5,1),
    blood_type     varchar(10),
    phone          varchar(30),
    id_card_cipher varchar(255),
    avatar_url     varchar(500),
    remark         varchar(500),
    sort           int          NOT NULL DEFAULT 0,
    created_at     timestamp  NOT NULL DEFAULT now(),
    updated_at     timestamp  NOT NULL DEFAULT now(),
    deleted        smallint     NOT NULL DEFAULT 0,
    CONSTRAINT pk_family_member PRIMARY KEY (id)
);
CREATE INDEX idx_member_user ON family_member (user_id, deleted);

CREATE TABLE member_medical_history (
    id              bigint       NOT NULL,
    member_id       bigint       NOT NULL,
    disease_name    varchar(100) NOT NULL,
    diagnosed_date  date,
    disease_status  varchar(20)  NOT NULL,
    treatment_desc  varchar(500),
    hospital        varchar(100),
    remark          varchar(500),
    created_at      timestamp  NOT NULL DEFAULT now(),
    updated_at      timestamp  NOT NULL DEFAULT now(),
    deleted         smallint     NOT NULL DEFAULT 0,
    CONSTRAINT pk_member_medical_history PRIMARY KEY (id),
    CONSTRAINT fk_medical_history_member FOREIGN KEY (member_id) REFERENCES family_member (id)
);
CREATE INDEX idx_mh_member ON member_medical_history (member_id, deleted);

CREATE TABLE member_allergy (
    id           bigint       NOT NULL,
    member_id    bigint       NOT NULL,
    allergen     varchar(100) NOT NULL,
    allergy_type varchar(20)  NOT NULL,
    reaction     varchar(200),
    severity     varchar(10),
    remark       varchar(500),
    created_at   timestamp  NOT NULL DEFAULT now(),
    updated_at   timestamp  NOT NULL DEFAULT now(),
    deleted      smallint     NOT NULL DEFAULT 0,
    CONSTRAINT pk_member_allergy PRIMARY KEY (id),
    CONSTRAINT fk_allergy_member FOREIGN KEY (member_id) REFERENCES family_member (id)
);
CREATE INDEX idx_allergy_member ON member_allergy (member_id, deleted);

CREATE TABLE member_medication (
    id            bigint       NOT NULL,
    member_id     bigint       NOT NULL,
    drug_name     varchar(100) NOT NULL,
    dose          varchar(50),
    frequency     varchar(50),
    route         varchar(20),
    start_date    date,
    end_date      date,
    purpose       varchar(100),
    status        varchar(20)  NOT NULL DEFAULT 'TAKING',
    doctor_advice varchar(500),
    remark        varchar(500),
    created_at    timestamp  NOT NULL DEFAULT now(),
    updated_at    timestamp  NOT NULL DEFAULT now(),
    deleted       smallint     NOT NULL DEFAULT 0,
    CONSTRAINT pk_member_medication PRIMARY KEY (id),
    CONSTRAINT fk_medication_member FOREIGN KEY (member_id) REFERENCES family_member (id)
);
CREATE INDEX idx_medication_member ON member_medication (member_id, deleted);

CREATE TABLE member_family_history (
    id           bigint       NOT NULL,
    member_id    bigint       NOT NULL,
    relation     varchar(20)  NOT NULL,
    disease_name varchar(100) NOT NULL,
    onset_age    int,
    remark       varchar(500),
    created_at   timestamp  NOT NULL DEFAULT now(),
    updated_at   timestamp  NOT NULL DEFAULT now(),
    deleted      smallint     NOT NULL DEFAULT 0,
    CONSTRAINT pk_member_family_history PRIMARY KEY (id),
    CONSTRAINT fk_family_history_member FOREIGN KEY (member_id) REFERENCES family_member (id)
);
CREATE INDEX idx_family_history_member ON member_family_history (member_id, deleted);

CREATE TABLE member_lifestyle (
    id                 bigint       NOT NULL,
    member_id          bigint       NOT NULL,
    smoking            varchar(20),
    smoking_amount     varchar(50),
    drinking           varchar(20),
    drinking_amount    varchar(50),
    sleep_hours        numeric(3,1),
    sleep_quality      varchar(10),
    exercise_frequency varchar(50),
    sedentary_hours    numeric(3,1),
    diet_habit         varchar(255),
    stress_level       varchar(10),
    updated_at         timestamp  NOT NULL DEFAULT now(),
    CONSTRAINT pk_member_lifestyle PRIMARY KEY (id),
    CONSTRAINT fk_lifestyle_member FOREIGN KEY (member_id) REFERENCES family_member (id)
);
CREATE UNIQUE INDEX uk_lifestyle_member ON member_lifestyle (member_id);

CREATE TABLE metric_category (
    id     bigint      NOT NULL,
    code   varchar(50) NOT NULL,
    name   varchar(50) NOT NULL,
    sort   int         NOT NULL DEFAULT 0,
    status smallint    NOT NULL DEFAULT 1,
    CONSTRAINT pk_metric_category PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_metric_category_code ON metric_category (code);

CREATE TABLE metric_dict (
    id          bigint       NOT NULL,
    category_id bigint       NOT NULL,
    code        varchar(50)  NOT NULL,
    name        varchar(100) NOT NULL,
    name_en     varchar(100),
    alias       varchar(500),
    value_type  varchar(10)  NOT NULL DEFAULT 'NUMERIC',
    unit        varchar(30),
    description text,
    high_desc   text,
    low_desc    text,
    sort        int          NOT NULL DEFAULT 0,
    status      smallint     NOT NULL DEFAULT 1,
    created_at  timestamp  NOT NULL DEFAULT now(),
    updated_at  timestamp  NOT NULL DEFAULT now(),
    CONSTRAINT pk_metric_dict PRIMARY KEY (id),
    CONSTRAINT fk_metric_dict_category FOREIGN KEY (category_id) REFERENCES metric_category (id)
);
CREATE UNIQUE INDEX uk_metric_code ON metric_dict (code);
CREATE INDEX idx_metric_category ON metric_dict (category_id);
CREATE INDEX idx_metric_name ON metric_dict (name);

CREATE TABLE metric_reference_range (
    id          bigint        NOT NULL,
    metric_id   bigint        NOT NULL,
    gender      varchar(5)    NOT NULL DEFAULT 'ALL',
    age_min     int           NOT NULL DEFAULT 0,
    age_max     int           NOT NULL DEFAULT 150,
    ref_min     numeric(12,3),
    ref_max     numeric(12,3),
    ref_text    varchar(100),
    normal_text varchar(100),
    status      smallint      NOT NULL DEFAULT 1,
    CONSTRAINT pk_metric_reference_range PRIMARY KEY (id),
    CONSTRAINT fk_range_metric FOREIGN KEY (metric_id) REFERENCES metric_dict (id)
);
CREATE INDEX idx_range_metric ON metric_reference_range (metric_id);

CREATE TABLE file_object (
    id            bigint       NOT NULL,
    user_id       bigint       NOT NULL,
    member_id     bigint,
    biz_type      varchar(20)  NOT NULL,
    original_name varchar(255),
    storage_type  varchar(10)  NOT NULL DEFAULT 'SUPABASE',
    bucket        varchar(50),
    object_key    varchar(500),
    file_size     bigint,
    mime_type     varchar(100),
    sha256        varchar(64),
    status        varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    timestamp  NOT NULL DEFAULT now(),
    deleted       smallint     NOT NULL DEFAULT 0,
    CONSTRAINT pk_file_object PRIMARY KEY (id)
);
CREATE INDEX idx_file_user ON file_object (user_id);
CREATE INDEX idx_file_member_biz ON file_object (member_id, biz_type);

CREATE TABLE exam_report (
    id              bigint       NOT NULL,
    member_id       bigint       NOT NULL,
    user_id         bigint       NOT NULL,
    report_name     varchar(100) NOT NULL,
    report_date     date         NOT NULL,
    institution     varchar(100),
    report_type     varchar(20)  NOT NULL DEFAULT 'ANNUAL',
    source_type     varchar(10)  NOT NULL DEFAULT 'MANUAL',
    file_id         bigint,
    parse_status    varchar(20)  NOT NULL DEFAULT 'UPLOADED',
    indicator_count int          NOT NULL DEFAULT 0,
    abnormal_count  int          NOT NULL DEFAULT 0,
    ai_status       varchar(20)  NOT NULL DEFAULT 'NONE',
    remark          varchar(500),
    created_at      timestamp  NOT NULL DEFAULT now(),
    updated_at      timestamp  NOT NULL DEFAULT now(),
    deleted         smallint     NOT NULL DEFAULT 0,
    CONSTRAINT pk_exam_report PRIMARY KEY (id),
    CONSTRAINT fk_report_member FOREIGN KEY (member_id) REFERENCES family_member (id),
    CONSTRAINT fk_report_file FOREIGN KEY (file_id) REFERENCES file_object (id)
);
CREATE INDEX idx_report_member_date ON exam_report (member_id, report_date DESC);
CREATE INDEX idx_report_user ON exam_report (user_id);

CREATE TABLE exam_indicator (
    id            bigint        NOT NULL,
    report_id     bigint        NOT NULL,
    member_id     bigint        NOT NULL,
    metric_id     bigint,
    metric_code   varchar(50),
    metric_name   varchar(100)  NOT NULL,
    category_name varchar(50),
    value_text    varchar(100),
    value_numeric numeric(12,3),
    unit          varchar(30),
    ref_range_text varchar(100),
    ref_min       numeric(12,3),
    ref_max       numeric(12,3),
    judge_status  varchar(20)   NOT NULL DEFAULT 'UNKNOWN',
    judge_note    varchar(200),
    match_type    varchar(10),
    source        varchar(10)   NOT NULL DEFAULT 'MANUAL',
    sort          int           NOT NULL DEFAULT 0,
    created_at    timestamp   NOT NULL DEFAULT now(),
    CONSTRAINT pk_exam_indicator PRIMARY KEY (id),
    CONSTRAINT fk_indicator_report FOREIGN KEY (report_id) REFERENCES exam_report (id),
    CONSTRAINT fk_indicator_metric FOREIGN KEY (metric_id) REFERENCES metric_dict (id)
);
CREATE INDEX idx_ind_report ON exam_indicator (report_id);
CREATE INDEX idx_ind_member_metric ON exam_indicator (member_id, metric_id);

CREATE TABLE ai_interpretation (
    id             bigint      NOT NULL,
    report_id      bigint      NOT NULL,
    member_id      bigint      NOT NULL,
    user_id        bigint      NOT NULL,
    status         varchar(20) NOT NULL DEFAULT 'PROCESSING',
    risk_level     varchar(10),
    summary        text,
    content_json   jsonb,
    content_md     text,
    abnormal_count int         NOT NULL DEFAULT 0,
    focus_metrics  jsonb,
    model          varchar(50),
    prompt_version int,
    token_in       int         NOT NULL DEFAULT 0,
    token_out      int         NOT NULL DEFAULT 0,
    duration_ms    int,
    error_msg      text,
    created_at     timestamp NOT NULL DEFAULT now(),
    updated_at     timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_interpretation PRIMARY KEY (id),
    CONSTRAINT fk_interpretation_report FOREIGN KEY (report_id) REFERENCES exam_report (id)
);
CREATE INDEX idx_interp_report ON ai_interpretation (report_id);
CREATE INDEX idx_interp_member ON ai_interpretation (member_id);

CREATE TABLE ai_suggestion (
    id                    bigint      NOT NULL,
    member_id             bigint      NOT NULL,
    user_id               bigint      NOT NULL,
    suggestion_type       varchar(20) NOT NULL,
    source_type           varchar(20) NOT NULL,
    source_id             bigint,
    goal                  varchar(30),
    status                varchar(20) NOT NULL DEFAULT 'PROCESSING',
    assessment_md         text,
    weekly_plan_json      jsonb,
    rehab_json            jsonb,
    precautions_md        text,
    progress_metrics_json jsonb,
    model                 varchar(50),
    prompt_version        int,
    token_in              int         NOT NULL DEFAULT 0,
    token_out             int         NOT NULL DEFAULT 0,
    duration_ms           int,
    error_msg             text,
    created_at            timestamp NOT NULL DEFAULT now(),
    updated_at            timestamp NOT NULL DEFAULT now(),
    deleted               smallint    NOT NULL DEFAULT 0,
    CONSTRAINT pk_ai_suggestion PRIMARY KEY (id),
    CONSTRAINT fk_suggestion_member FOREIGN KEY (member_id) REFERENCES family_member (id)
);
CREATE INDEX idx_suggestion_member ON ai_suggestion (member_id, deleted);

CREATE TABLE exercise_daily (
    id                bigint        NOT NULL,
    member_id         bigint        NOT NULL,
    user_id           bigint        NOT NULL,
    record_date       date          NOT NULL,
    steps             int,
    active_minutes    int,
    calories_kcal     numeric(7,1),
    distance_km       numeric(6,2),
    sleep_hours       numeric(3,1),
    sleep_quality     varchar(10),
    resting_heart_rate int,
    weight_kg         numeric(5,1),
    systolic_bp       int,
    diastolic_bp      int,
    blood_glucose     numeric(4,1),
    glucose_type      varchar(10),
    mood              varchar(10),
    note              varchar(500),
    created_at        timestamp   NOT NULL DEFAULT now(),
    updated_at        timestamp   NOT NULL DEFAULT now(),
    deleted           smallint      NOT NULL DEFAULT 0,
    CONSTRAINT pk_exercise_daily PRIMARY KEY (id),
    CONSTRAINT fk_daily_member FOREIGN KEY (member_id) REFERENCES family_member (id)
);
CREATE UNIQUE INDEX uk_daily_member_date ON exercise_daily (member_id, record_date) WHERE deleted = 0;
CREATE INDEX idx_daily_date ON exercise_daily (record_date);

CREATE TABLE exercise_workout (
    id               bigint        NOT NULL,
    member_id        bigint        NOT NULL,
    user_id          bigint        NOT NULL,
    workout_date     date          NOT NULL,
    start_time       time,
    workout_type     varchar(20)   NOT NULL,
    title            varchar(100),
    duration_min     int,
    intensity        varchar(10),
    distance_km      numeric(6,2),
    avg_heart_rate   int,
    max_heart_rate   int,
    pace_sec_per_km  int,
    calories_kcal    numeric(7,1),
    feeling          varchar(10),
    note             varchar(500),
    created_at       timestamp   NOT NULL DEFAULT now(),
    updated_at       timestamp   NOT NULL DEFAULT now(),
    deleted          smallint      NOT NULL DEFAULT 0,
    CONSTRAINT pk_exercise_workout PRIMARY KEY (id),
    CONSTRAINT fk_workout_member FOREIGN KEY (member_id) REFERENCES family_member (id)
);
CREATE INDEX idx_workout_member_date ON exercise_workout (member_id, workout_date DESC);
CREATE INDEX idx_workout_type ON exercise_workout (workout_type);

CREATE TABLE chat_session (
    id              bigint       NOT NULL,
    member_id       bigint       NOT NULL,
    user_id         bigint       NOT NULL,
    title           varchar(100) NOT NULL DEFAULT '新对话',
    last_message_at timestamp,
    message_count   int          NOT NULL DEFAULT 0,
    created_at      timestamp  NOT NULL DEFAULT now(),
    updated_at      timestamp  NOT NULL DEFAULT now(),
    deleted         smallint     NOT NULL DEFAULT 0,
    CONSTRAINT pk_chat_session PRIMARY KEY (id),
    CONSTRAINT fk_session_member FOREIGN KEY (member_id) REFERENCES family_member (id)
);
CREATE INDEX idx_session_member ON chat_session (member_id, last_message_at DESC);
CREATE INDEX idx_session_user ON chat_session (user_id);

CREATE TABLE chat_message (
    id          bigint       NOT NULL,
    session_id  bigint       NOT NULL,
    member_id   bigint       NOT NULL,
    user_id     bigint       NOT NULL,
    role        varchar(20)  NOT NULL,
    content     text         NOT NULL,
    status      varchar(20)  NOT NULL DEFAULT 'DONE',
    model       varchar(50),
    token_in    int          NOT NULL DEFAULT 0,
    token_out   int          NOT NULL DEFAULT 0,
    duration_ms int,
    error_msg   text,
    created_at  timestamp  NOT NULL DEFAULT now(),
    CONSTRAINT pk_chat_message PRIMARY KEY (id),
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_session (id)
);
CREATE INDEX idx_msg_session ON chat_message (session_id, created_at);

CREATE TABLE ai_async_task (
    id           bigint       NOT NULL,
    user_id      bigint       NOT NULL,
    member_id    bigint,
    task_type    varchar(30)  NOT NULL,
    biz_type     varchar(30),
    biz_id       bigint,
    status       varchar(20)  NOT NULL DEFAULT 'PENDING',
    progress     int          NOT NULL DEFAULT 0,
    payload_json jsonb,
    result_json  jsonb,
    retry_count  int          NOT NULL DEFAULT 0,
    max_retry    int          NOT NULL DEFAULT 2,
    error_msg    text,
    started_at   timestamp,
    finished_at  timestamp,
    created_at   timestamp  NOT NULL DEFAULT now(),
    updated_at   timestamp  NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_async_task PRIMARY KEY (id)
);
CREATE INDEX idx_task_status_type ON ai_async_task (status, task_type);
CREATE INDEX idx_task_biz ON ai_async_task (biz_type, biz_id);

CREATE TABLE ai_call_log (
    id                bigint         NOT NULL,
    user_id           bigint,
    task_id           bigint,
    scene             varchar(20)    NOT NULL,
    model             varchar(50),
    request_id        varchar(64),
    prompt_tokens     int            NOT NULL DEFAULT 0,
    completion_tokens int            NOT NULL DEFAULT 0,
    total_tokens      int            NOT NULL DEFAULT 0,
    cost_estimate     numeric(10,6)  NOT NULL DEFAULT 0,
    duration_ms       int,
    is_stream         smallint       NOT NULL DEFAULT 0,
    status            varchar(20)    NOT NULL DEFAULT 'SUCCESS',
    error_code        varchar(50),
    error_msg         text,
    created_at        timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_call_log PRIMARY KEY (id)
);
CREATE INDEX idx_log_scene_time ON ai_call_log (scene, created_at);
CREATE INDEX idx_log_user ON ai_call_log (user_id);
CREATE INDEX idx_log_task ON ai_call_log (task_id);

CREATE TABLE sys_prompt_template (
    id              bigint       NOT NULL,
    code            varchar(50)  NOT NULL,
    name            varchar(100) NOT NULL,
    scene           varchar(30)  NOT NULL,
    version         int          NOT NULL,
    content         text         NOT NULL,
    variables_json  jsonb,
    temperature     numeric(3,2) NOT NULL DEFAULT 0.30,
    max_tokens      int          NOT NULL DEFAULT 4096,
    status          varchar(20)  NOT NULL DEFAULT 'ENABLED',
    is_default      smallint     NOT NULL DEFAULT 0,
    created_at      timestamp  NOT NULL DEFAULT now(),
    updated_at      timestamp  NOT NULL DEFAULT now(),
    CONSTRAINT pk_sys_prompt_template PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_template_code_version ON sys_prompt_template (code, version);

CREATE TABLE sys_audit_log (
    id            bigint       NOT NULL,
    user_id       bigint,
    username      varchar(50),
    action        varchar(50)  NOT NULL,
    resource_type varchar(50),
    resource_id   bigint,
    detail_json   jsonb,
    ip            varchar(45),
    user_agent    varchar(300),
    created_at    timestamp  NOT NULL DEFAULT now(),
    CONSTRAINT pk_sys_audit_log PRIMARY KEY (id)
);
CREATE INDEX idx_audit_user_time ON sys_audit_log (user_id, created_at);
CREATE INDEX idx_audit_action ON sys_audit_log (action);

CREATE TABLE export_record (
    id          bigint      NOT NULL,
    user_id     bigint      NOT NULL,
    member_id   bigint      NOT NULL,
    export_type varchar(30) NOT NULL,
    ref_id      bigint,
    file_id     bigint,
    status      varchar(20) NOT NULL DEFAULT 'PROCESSING',
    error_msg   text,
    created_at  timestamp NOT NULL DEFAULT now(),
    finished_at timestamp,
    CONSTRAINT pk_export_record PRIMARY KEY (id),
    CONSTRAINT fk_export_member FOREIGN KEY (member_id) REFERENCES family_member (id),
    CONSTRAINT fk_export_file FOREIGN KEY (file_id) REFERENCES file_object (id)
);
CREATE INDEX idx_export_user ON export_record (user_id);
CREATE INDEX idx_export_member ON export_record (member_id);
