<?php
declare(strict_types=1);

require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/db.php';
require_once __DIR__ . '/subscriptions.php';

$userId = requireAuth();
$message = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && ($_POST['action'] ?? '') === 'grant_trial') {
    grantTrialSubscription($pdo, $userId);
    $message = 'Тестовая подписка выдана на 30 дней.';
}

$userStmt = $pdo->prepare(
    "SELECT id, login
     FROM users
     WHERE id = :id
     LIMIT 1"
);
$userStmt->execute(['id' => $userId]);
$user = $userStmt->fetch();

if (!$user) {
    session_destroy();
    header('Location: login.php');
    exit;
}

$subscription = fetchSubscription($pdo, $userId);
$active = isSubscriptionActive($subscription);
?>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Личный кабинет</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
<main class="dashboard-shell">
    <section class="card dashboard-card">
        <div class="dashboard-head">
            <div>
                <h1>Личный кабинет</h1>
                <p class="muted">Привет, <?= htmlspecialchars((string) $user['login'], ENT_QUOTES, 'UTF-8') ?></p>
            </div>
            <a class="btn btn-outline" href="logout.php">Выйти</a>
        </div>

        <?php if ($message !== ''): ?>
            <div class="alert alert-success"><?= htmlspecialchars($message, ENT_QUOTES, 'UTF-8') ?></div>
        <?php endif; ?>

        <div class="info-grid">
            <article class="info-card">
                <h3>UID пользователя</h3>
                <p class="uid">#<?= (int) $user['id'] ?></p>
            </article>
            <article class="info-card">
                <h3>Подписка</h3>
                <p class="<?= $active ? 'status active' : 'status inactive' ?>">
                    <?= $active ? 'Активна' : 'Неактивна' ?>
                </p>
                <?php if ($subscription !== null): ?>
                    <p class="muted small">
                        План: <?= htmlspecialchars((string) $subscription['plan'], ENT_QUOTES, 'UTF-8') ?> |
                        До: <?= htmlspecialchars((string) $subscription['expires_at'], ENT_QUOTES, 'UTF-8') ?>
                    </p>
                <?php endif; ?>
            </article>
        </div>

        <form method="post" class="trial-form">
            <input type="hidden" name="action" value="grant_trial">
            <button type="submit" class="btn btn-primary">Получить тестовую подписку</button>
        </form>
    </section>
</main>
</body>
</html>
