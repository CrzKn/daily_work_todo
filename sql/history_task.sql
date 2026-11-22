-- ============================================================================
-- 历史任务回看模块 - 数据库变更脚本
-- 依据 requirement/requirement.md 3.7.6
-- 适用：MySQL 8.0
-- 变更内容：
--   1. pt_status 新增 is_history：标记该状态下的任务是否计入历史回看
--   2. personal_task 新增 archived_time：进入历史状态时写入，离开时清空
--   3. 初始化：预置「已完成」「已归档」默认计入历史回看
-- 幂等说明：使用 INFORMATION_SCHEMA 判断列是否存在，可重复执行。
-- ============================================================================

-- 1. pt_status 新增 is_history
SET @col_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pt_status'
    AND COLUMN_NAME = 'is_history'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `pt_status` ADD COLUMN `is_history` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否计入历史回看：1计入、0不计入''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. personal_task 新增 archived_time
SET @col_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'personal_task'
    AND COLUMN_NAME = 'archived_time'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `personal_task` ADD COLUMN `archived_time` DATETIME NULL DEFAULT NULL COMMENT ''归档时间，进入历史状态时写入''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 初始化：预置「已完成」「已归档」默认计入历史回看（按实际名称调整）
UPDATE `pt_status`
SET `is_history` = 1
WHERE `delete_status` = 0
  AND `pt_status_name` IN ('已完成', '已归档')
  AND (`is_history` = 0 OR `is_history` IS NULL);
