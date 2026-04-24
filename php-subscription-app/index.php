<?php
declare(strict_types=1);

require_once __DIR__ . '/auth.php';

if (currentUserId() !== null) {
    header('Location: dashboard.php');
    exit;
}

header('Location: login.php');
exit;
