using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using DantesUidSite.Services;

namespace DantesUidSite.Pages;

[Authorize]
public class DashboardModel : PageModel
{
    private readonly UserAuthService _authService;

    public DashboardModel(UserAuthService authService)
    {
        _authService = authService;
    }

    public string UserUid { get; private set; } = string.Empty;
    public string Login { get; private set; } = string.Empty;
    public string Email { get; private set; } = string.Empty;
    public string Subscription { get; private set; } = string.Empty;
    public string CreatedAtUtc { get; private set; } = string.Empty;

    public IActionResult OnGet()
    {
        var login = User.Identity?.Name;
        if (string.IsNullOrWhiteSpace(login))
        {
            return RedirectToPage("/Login");
        }

        var user = _authService.GetByLogin(login);
        if (user is null)
        {
            return RedirectToPage("/Login");
        }

        UserUid = user.Uid;
        Login = user.Login;
        Email = user.Email;
        Subscription = user.Subscription;
        CreatedAtUtc = user.CreatedAtUtc.ToString("yyyy-MM-dd HH:mm:ss");

        return Page();
    }
}
