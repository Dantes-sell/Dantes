<?php
declare(strict_types=1);

require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/db.php';

if (currentUserId() !== null) {
    header('Location: dashboard.php');
    exit;
}

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $login = trim((string) ($_POST['login'] ?? ''));
    $password = (string) ($_POST['password'] ?? '');

    if ($login === '' || $password === '') {
        $error = 'Заполни логин и пароль.';
    } else {
        $stmt = $pdo->prepare(
            "SELECT id, password_hash
             FROM users
             WHERE login = :login
             LIMIT 1"
        );
        $stmt->execute(['login' => $login]);
        $user = $stmt->fetch();

        if ($user && password_verify($password, (string) $user['password_hash'])) {
            $_SESSION['user_id'] = (int) $user['id'];
            header('Location: dashboard.php');
            exit;
        }

        $error = 'Неверный логин или пароль.';
    }
}
?>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Вход</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
<main class="auth-shell">
    <section class="card">
        <h1>Вход</h1>
        <p class="muted">Войди в личный кабинет пользователя.</p>

        <?php if ($error !== ''): ?>
            <div class="alert alert-error"><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></div>
        <?php endif; ?>

        <form method="post" class="form">
            <label>
                Логин
                <input type="text" name="login" required maxlength="64">
            </label>
            <label>
                Пароль
                <input type="password" name="password" required>
            </label>
            <button type="submit" class="btn btn-primary">Войти</button>
        </form>

        <p class="muted bottom-link">Нет аккаунта? <a href="register.php">Зарегистрироваться</a></p>
    </section>
</main>
</body>
</html>
