namespace DantesUidSite.Models;

public sealed class UserRecord
{
    public int Id { get; init; }
    public string Uid { get; init; } = string.Empty;
    public string Email { get; init; } = string.Empty;
    public string Login { get; init; } = string.Empty;
    public string Subscription { get; init; } = "No subscription";
    public DateTime CreatedAtUtc { get; init; }
}

