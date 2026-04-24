<?php
declare(strict_types=1);

function grantTrialSubscription(PDO $pdo, int $userId): void
{
    $stmt = $pdo->prepare(
        "INSERT INTO subscriptions (user_id, plan, expires_at)
         VALUES (:user_id, 'basic', DATE_ADD(NOW(), INTERVAL 30 DAY))
         ON DUPLICATE KEY UPDATE
            plan = VALUES(plan),
            expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY)"
    );
    $stmt->execute(['user_id' => $userId]);
}

function fetchSubscription(PDO $pdo, int $userId): ?array
{
    $stmt = $pdo->prepare(
        "SELECT id, user_id, plan, expires_at
         FROM subscriptions
         WHERE user_id = :user_id
         LIMIT 1"
    );
    $stmt->execute(['user_id' => $userId]);

    $subscription = $stmt->fetch();
    return $subscription ?: null;
}

function isSubscriptionActive(?array $subscription): bool
{
    if ($subscription === null || empty($subscription['expires_at'])) {
        return false;
    }

    return strtotime((string) $subscription['expires_at']) > time();
}
