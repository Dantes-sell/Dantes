using System.Security.Cryptography;
using Microsoft.Data.Sqlite;
using DantesUidSite.Models;

namespace DantesUidSite.Services;

public sealed class UserAuthService
{
    private readonly string _connectionString;

    public UserAuthService(IWebHostEnvironment env)
    {
        var dataDir = Path.Combine(env.ContentRootPath, "Data");
        Directory.CreateDirectory(dataDir);
        var dbPath = Path.Combine(dataDir, "dantes-users.db");
        _connectionString = $"Data Source={dbPath}";
    }

    public void Initialize()
    {
        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        var cmd = conn.CreateCommand();
        cmd.CommandText =
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uid TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL UNIQUE,
                login TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                subscription TEXT NOT NULL DEFAULT 'No subscription',
                created_at_utc TEXT NOT NULL
            );
            """;
        cmd.ExecuteNonQuery();
    }

    public bool Register(string email, string login, string password, out string error, out string uid)
    {
        uid = string.Empty;
        error = string.Empty;

        if (string.IsNullOrWhiteSpace(email) || string.IsNullOrWhiteSpace(login) || string.IsNullOrWhiteSpace(password))
        {
            error = "Fill all fields.";
            return false;
        }

        if (password.Length < 6)
        {
            error = "Password must be at least 6 characters.";
            return false;
        }

        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        using (var check = conn.CreateCommand())
        {
            check.CommandText =
                """
                SELECT 1
                FROM users
                WHERE lower(email) = lower($email)
                   OR lower(login) = lower($login)
                LIMIT 1;
                """;
            check.Parameters.AddWithValue("$email", email.Trim());
            check.Parameters.AddWithValue("$login", login.Trim());

            var exists = check.ExecuteScalar();
            if (exists != null)
            {
                error = "Email or login already exists.";
                return false;
            }
        }

        var salt = RandomNumberGenerator.GetBytes(16);
        var hash = HashPassword(password, salt);
        var createdAtUtc = DateTime.UtcNow.ToString("O");

        using var tx = conn.BeginTransaction();
        using var insert = conn.CreateCommand();
        insert.Transaction = tx;
        insert.CommandText =
            """
            INSERT INTO users (uid, email, login, password_hash, password_salt, created_at_utc)
            VALUES ($uid, $email, $login, $hash, $salt, $createdAtUtc);
            SELECT last_insert_rowid();
            """;

        // temporary uid, will be replaced by sequential formatted uid from row id
        insert.Parameters.AddWithValue("$uid", "PENDING");
        insert.Parameters.AddWithValue("$email", email.Trim());
        insert.Parameters.AddWithValue("$login", login.Trim());
        insert.Parameters.AddWithValue("$hash", Convert.ToBase64String(hash));
        insert.Parameters.AddWithValue("$salt", Convert.ToBase64String(salt));
        insert.Parameters.AddWithValue("$createdAtUtc", createdAtUtc);

        var rowIdObj = insert.ExecuteScalar();
        var rowId = Convert.ToInt32(rowIdObj);
        uid = $"DNT-{rowId:000000}";

        using var update = conn.CreateCommand();
        update.Transaction = tx;
        update.CommandText = "UPDATE users SET uid = $uid WHERE id = $id;";
        update.Parameters.AddWithValue("$uid", uid);
        update.Parameters.AddWithValue("$id", rowId);
        update.ExecuteNonQuery();

        tx.Commit();
        return true;
    }

    public bool ValidateCredentials(string identifier, string password, out UserRecord? user)
    {
        user = null;
        if (string.IsNullOrWhiteSpace(identifier) || string.IsNullOrWhiteSpace(password))
        {
            return false;
        }

        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        using var cmd = conn.CreateCommand();
        cmd.CommandText =
            """
            SELECT id, uid, email, login, password_hash, password_salt, subscription, created_at_utc
            FROM users
            WHERE lower(email) = lower($identifier)
               OR lower(login) = lower($identifier)
            LIMIT 1;
            """;
        cmd.Parameters.AddWithValue("$identifier", identifier.Trim());

        using var reader = cmd.ExecuteReader();
        if (!reader.Read())
        {
            return false;
        }

        var hashDb = reader.GetString(4);
        var saltDb = reader.GetString(5);
        var salt = Convert.FromBase64String(saltDb);
        var computed = HashPassword(password, salt);
        var computedB64 = Convert.ToBase64String(computed);

        if (!CryptographicOperations.FixedTimeEquals(
                Convert.FromBase64String(hashDb),
                Convert.FromBase64String(computedB64)))
        {
            return false;
        }

        user = new UserRecord
        {
            Id = reader.GetInt32(0),
            Uid = reader.GetString(1),
            Email = reader.GetString(2),
            Login = reader.GetString(3),
            Subscription = reader.GetString(6),
            CreatedAtUtc = DateTime.Parse(reader.GetString(7))
        };
        return true;
    }

    public UserRecord? GetByLogin(string login)
    {
        if (string.IsNullOrWhiteSpace(login))
        {
            return null;
        }

        using var conn = new SqliteConnection(_connectionString);
        conn.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText =
            """
            SELECT id, uid, email, login, subscription, created_at_utc
            FROM users
            WHERE lower(login) = lower($login)
            LIMIT 1;
            """;
        cmd.Parameters.AddWithValue("$login", login.Trim());

        using var reader = cmd.ExecuteReader();
        if (!reader.Read())
        {
            return null;
        }

        return new UserRecord
        {
            Id = reader.GetInt32(0),
            Uid = reader.GetString(1),
            Email = reader.GetString(2),
            Login = reader.GetString(3),
            Subscription = reader.GetString(4),
            CreatedAtUtc = DateTime.Parse(reader.GetString(5))
        };
    }

    private static byte[] HashPassword(string password, byte[] salt)
    {
        return Rfc2898DeriveBytes.Pbkdf2(password, salt, 100_000, HashAlgorithmName.SHA256, 32);
    }
}

