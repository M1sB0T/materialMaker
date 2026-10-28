-- 建表脚本：幂等，可重复执行（本地启动由 Spring Boot sql init 执行；
-- 容器环境同时挂载到 MySQL /docker-entrypoint-initdb.d）

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS auth_tokens (
    token      VARCHAR(64) PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS templates (
    id                   VARCHAR(64)  PRIMARY KEY,
    title                VARCHAR(100) NOT NULL,
    category             VARCHAR(50)  NOT NULL,
    cover_url            VARCHAR(255) NOT NULL,
    description          VARCHAR(500) NOT NULL,
    required_image_count INT          NOT NULL,
    accepted_mime_types  VARCHAR(255) NOT NULL,
    max_file_size_bytes  BIGINT       NOT NULL,
    result_asset         VARCHAR(255) NOT NULL,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS materials (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_path   VARCHAR(500) NOT NULL,
    mime_type     VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_material_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS tasks (
    id            VARCHAR(64)  PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    template_id   VARCHAR(64)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    current_phase VARCHAR(30)  NULL,
    fail_phase    VARCHAR(30)  NULL,
    fail_reason   VARCHAR(500) NULL,
    result_url    VARCHAR(255) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    finished_at   DATETIME     NULL,
    INDEX idx_task_user (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS task_steps (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id     VARCHAR(64)  NOT NULL,
    step        VARCHAR(30)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    message     VARCHAR(500) NULL,
    started_at  DATETIME     NULL,
    finished_at DATETIME     NULL,
    INDEX idx_step_task (task_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
