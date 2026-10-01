-- P2 认证鉴权：管理员口令 BCrypt 化 + 角色值归一
-- 目标库：本机 online_exam（MySQL 9.6）
-- 可回退：docs/db/baseline/online_exam_data_baseline.sql 里存有原明文 admin123
-- 约定口令：admin / admin123（仅此本地项目，勿用于任何联网环境）

UPDATE users
SET password = '$2a$10$tHd7EVaxvZLNdRg9ok/YauAEFsLo0AZlrxt5Spk1SbIZdzGBJenpG',
    role = UPPER(role)
WHERE username = 'admin';

-- 校验：password 前缀应为 $2a$10$，role 应为 ADMIN
SELECT id, username, LEFT(password, 7) AS pwd_prefix, CHAR_LENGTH(password) AS pwd_len, role, status
FROM users
WHERE username = 'admin';
