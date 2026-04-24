<?php
declare(strict_types=1);

require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/db.php';

if (currentUserId() !== null) {
    header('Location: dashboard.php');
    exit;
}

$error = '';
$success = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $login = trim((string) ($_POST['login'] ?? ''));
    $password = (string) ($_POST['password'] ?? '');

    if ($login === '' || $password === '') {
        $error = 'Заполни логин и пароль.';
    } elseif (mb_strlen($login) < 3) {
        $error = 'Логин должен быть не короче 3 символов.';
    } elseif (strlen($password) < 6) {
        $error = 'Пароль должен быть не короче 6 символов.';
    } else {
        $passwordHash = password_hash($password, PASSWORD_DEFAULT);
        $stmt = $pdo->prepare(
            "INSERT INTO users (login, password_hash)
             VALUES (:login, :password_hash)"
        );

        try {
            $stmt->execute([
                'login' => $login,
                'password_hash' => $passwordHash,
            ]);
            $success = 'Регистрация успешна. Теперь войди в аккаунт.';
        } catch (PDOException $e) {
            $error = 'Такой логин уже существует.';
        }
    }
}
?>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Регистрация</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
<main class="auth-shell">
    <section class="card">
        <h1>Регистрация</h1>
        <p class="muted">Создай аккаунт для получения UID и подписки.</p>

        <?php if ($error !== ''): ?>
            <div class="alert alert-error"><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></div>
        <?php endif; ?>

        <?php if ($success !== ''): ?>
            <div class="alert alert-success"><?= htmlspecialchars($success, ENT_QUOTES, 'UTF-8') ?></div>
        <?php endif; ?>

        <form method="post" class="form">
            <label>
                Логин
                <input type="text" name="login" required minlength="3" maxlength="64">
            </label>
            <label>
                Пароль
                <input type="password" name="password" required minlength="6">
            </label>
            <button type="submit" class="btn btn-primary">Зарегистрироваться</button>
        </form>

        <p class="muted bottom-link">Уже есть аккаунт? <a href="login.php">Войти</a></p>
    </section>
</main>
</body>
</html>
