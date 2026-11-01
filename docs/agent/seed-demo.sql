-- 仅在本机 tdtracking 执行；每次新增 200 条独立教学数据，不删除原有数据。
-- 时间锚点为当前整点，生成两个完整小时的数据，便于下一课比较。
SET @demo_end = DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00');
SET @demo_run = CONCAT('AGENT_DEMO_', REPLACE(UUID(), '-', ''));

START TRANSACTION;
INSERT INTO tdtracking.tracking_conversion_upload_log
    (message_id, callback_media, task_name, upload_status, is_upload_error,
     exec_num, created_at, updated_at)
SELECT
    CONCAT(@demo_run, '_', hours.h, '_', numbers.n),
    'APPSFLYER', 'Agent 教学样例',
    CASE
        WHEN hours.h = 2 THEN IF(numbers.n < 98, 20, -1)
        WHEN numbers.n < 80 THEN 20
        WHEN numbers.n < 90 THEN -1
        WHEN numbers.n < 95 THEN 0
        ELSE 10
    END,
    IF((hours.h = 2 AND numbers.n >= 98)
       OR (hours.h = 1 AND numbers.n >= 80 AND numbers.n < 90), 'Y', 'N'),
    1,
    TIMESTAMPADD(SECOND, numbers.n * 30 + 1, TIMESTAMPADD(HOUR, -hours.h, @demo_end)),
    TIMESTAMPADD(SECOND, numbers.n * 30 + 1, TIMESTAMPADD(HOUR, -hours.h, @demo_end))
FROM (SELECT 1 AS h UNION ALL SELECT 2) hours
CROSS JOIN (
    SELECT tens.n * 10 + units.n AS n
    FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) units
) numbers;
COMMIT;

SELECT @demo_run AS demo_run, @demo_end AS window_end;
SELECT upload_status, COUNT(*) AS total
FROM tdtracking.tracking_conversion_upload_log
WHERE message_id LIKE CONCAT(@demo_run, '%')
GROUP BY upload_status;
