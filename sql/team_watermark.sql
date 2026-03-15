/*
 Navicat Premium Data Transfer

 Source Server         : 本地数据库
 Source Server Type    : MySQL
 Source Server Version : 80044
 Source Host           : localhost:3306
 Source Schema         : team_watermark

 Target Server Type    : MySQL
 Target Server Version : 80044
 File Encoding         : 65001

 Date: 15/03/2026 18:45:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tw_batch_task
-- ----------------------------
DROP TABLE IF EXISTS `tw_batch_task`;
CREATE TABLE `tw_batch_task`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `task_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务编号',
  `team_id` int NOT NULL COMMENT '团队 ID',
  `created_by_id` int NOT NULL COMMENT '提交人 ID',
  `created_by_username` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提交人用户名快照',
  `user_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '提交人状态快照（active / renamed / left / deleted）',
  `template_id` int NOT NULL COMMENT '模板 ID',
  `template_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称快照',
  `template_version` int NOT NULL COMMENT '模板版本号快照',
  `template_snapshot` json NOT NULL COMMENT '模板完整快照',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '任务描述',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '任务图片总数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功处理数量',
  `failed_count` int NOT NULL DEFAULT 0 COMMENT '处理失败数量',
  `total_duration_ms` bigint NOT NULL DEFAULT 0 COMMENT '总处理耗时（毫秒）',
  `total_size` bigint NOT NULL DEFAULT 0 COMMENT '任务文件总大小（字节）',
  `result_zip_key` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '结果压缩包 MinIO Key',
  `report` json NULL COMMENT '任务处理报告',
  `started_at` datetime NULL DEFAULT NULL COMMENT '开始处理时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '完成处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bt_created_finished`(`created_by_id` ASC, `finished_at` ASC) USING BTREE,
  INDEX `idx_bt_team_finished_created`(`team_id` ASC, `finished_at` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '批量任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_black_list
-- ----------------------------
DROP TABLE IF EXISTS `tw_black_list`;
CREATE TABLE `tw_black_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型（token / email）',
  `value` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '值',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bl_type_value`(`type` ASC, `value` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 78 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '黑名单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_font
-- ----------------------------
DROP TABLE IF EXISTS `tw_font`;
CREATE TABLE `tw_font`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字体名称',
  `font_key` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字体文件 MinIO Key',
  `team_id` int NOT NULL COMMENT '所属团队 ID',
  `uploaded_by` int NOT NULL COMMENT '上传人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字体表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_payment_order
-- ----------------------------
DROP TABLE IF EXISTS `tw_payment_order`;
CREATE TABLE `tw_payment_order`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号',
  `team_id` int NOT NULL COMMENT '关联团队 ID',
  `user_id` int NULL DEFAULT NULL COMMENT '支付人 ID',
  `points` int NOT NULL COMMENT '购买点数',
  `amount` decimal(10, 2) NOT NULL COMMENT '订单金额',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '订单状态（pending / paid / invalid）',
  `alipay_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '支付宝交易号',
  `paid_at` datetime NULL DEFAULT NULL COMMENT '支付完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '支付订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_point_change_log
-- ----------------------------
DROP TABLE IF EXISTS `tw_point_change_log`;
CREATE TABLE `tw_point_change_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `team_id` int NOT NULL COMMENT '团队 ID',
  `change_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变动类型（recharge / deduct / refund）',
  `operator_user_id` int NULL DEFAULT NULL COMMENT '操作人用户 ID',
  `operator_username` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人用户名快照',
  `operator_user_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人状态快照（active / renamed / left / deleted）',
  `source_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务来源类型（payment / batch_task）',
  `source_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务来源标识',
  `points` int NOT NULL COMMENT '变动点数',
  `balance_before` int NOT NULL COMMENT '变动前点数余额',
  `balance_after` int NOT NULL COMMENT '变动后点数余额',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '变动说明',
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作 IP 地址',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pcl_source`(`source_type` ASC, `source_id` ASC, `change_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '点数流水日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_team
-- ----------------------------
DROP TABLE IF EXISTS `tw_team`;
CREATE TABLE `tw_team`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '团队名称',
  `point_balance` int NOT NULL DEFAULT 0 COMMENT '团队点数余额',
  `leader_id` int NOT NULL COMMENT '队长用户 ID',
  `owner_id` int NOT NULL COMMENT '归属用户 ID（团队创建者）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_team_event_log
-- ----------------------------
DROP TABLE IF EXISTS `tw_team_event_log`;
CREATE TABLE `tw_team_event_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `team_id` int NOT NULL COMMENT '团队 ID',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型',
  `operator_user_id` int NULL DEFAULT NULL COMMENT '操作人用户 ID',
  `operator_username` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人用户名快照',
  `operator_user_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人状态快照（active / renamed / left / deleted）',
  `affected_user_id` int NULL DEFAULT NULL COMMENT '受影响成员用户 ID',
  `affected_username` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '受影响成员用户名快照',
  `affected_user_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '受影响成员状态快照（active / renamed / left / deleted）',
  `invite_code_id` int NULL DEFAULT NULL COMMENT '关联邀请码 ID',
  `invite_code` char(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联邀请码快照',
  `before_data` json NULL COMMENT '变更前数据',
  `after_data` json NULL COMMENT '变更后数据',
  `details` json NULL COMMENT '事件扩展详情',
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作 IP 地址',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 54 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队变更日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_team_invite_code
-- ----------------------------
DROP TABLE IF EXISTS `tw_team_invite_code`;
CREATE TABLE `tw_team_invite_code`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `team_id` int NOT NULL COMMENT '团队 ID',
  `code` char(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邀请码',
  `valid_until` datetime NULL DEFAULT NULL COMMENT '有效期截止时间',
  `max_uses` int NULL DEFAULT NULL COMMENT '最大使用次数',
  `uses_count` int NULL DEFAULT 0 COMMENT '已使用次数',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'active' COMMENT '状态（active / inactive）',
  `created_by_id` int NULL DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队邀请码表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_team_member
-- ----------------------------
DROP TABLE IF EXISTS `tw_team_member`;
CREATE TABLE `tw_team_member`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `team_id` int NOT NULL COMMENT '团队 ID',
  `user_id` int NOT NULL COMMENT '用户 ID',
  `role` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'member' COMMENT '角色（leader / member）',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '加入团队时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tm_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_tm_team_id`(`team_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队成员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_user
-- ----------------------------
DROP TABLE IF EXISTS `tw_user`;
CREATE TABLE `tw_user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（加密后）',
  `email` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '绑定邮箱',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_watermark_resource_log
-- ----------------------------
DROP TABLE IF EXISTS `tw_watermark_resource_log`;
CREATE TABLE `tw_watermark_resource_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `team_id` int NOT NULL COMMENT '团队 ID',
  `resource_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资源范围（template / font）',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型',
  `operator_user_id` int NULL DEFAULT NULL COMMENT '操作人用户 ID',
  `operator_username` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人用户名快照',
  `operator_user_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人状态快照（active / renamed / left / deleted）',
  `resource_id` int NULL DEFAULT NULL COMMENT '资源 ID',
  `resource_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资源名称快照',
  `before_data` json NULL COMMENT '变更前数据',
  `after_data` json NULL COMMENT '变更后数据',
  `details` json NULL COMMENT '事件扩展详情',
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作 IP 地址',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '水印资源日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_watermark_template
-- ----------------------------
DROP TABLE IF EXISTS `tw_watermark_template`;
CREATE TABLE `tw_watermark_template`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `team_id` int NOT NULL COMMENT '团队 ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `config` json NOT NULL COMMENT '水印配置',
  `created_by_id` int NULL DEFAULT NULL COMMENT '创建人 ID',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '水印模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tw_watermark_template_draft
-- ----------------------------
DROP TABLE IF EXISTS `tw_watermark_template_draft`;
CREATE TABLE `tw_watermark_template_draft`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户 ID',
  `source_template_id` int NULL DEFAULT NULL COMMENT '源模板 ID（为空表示新建）',
  `source_version` int NULL DEFAULT NULL COMMENT '源模板版本号',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '草稿名称',
  `config` json NOT NULL COMMENT '草稿配置',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 119 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '水印模板草稿表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
