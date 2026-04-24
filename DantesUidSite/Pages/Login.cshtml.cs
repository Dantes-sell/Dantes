using System.ComponentModel.DataAnnotations;
using System.Security.Claims;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using DantesUidSite.Services;

namespace DantesUidSite.Pages;

public class LoginModel : PageModel
{
    private readonly UserAuthService _authService;

    public LoginModel(UserAuthService authService)
    {
        _authService = authService;
    }

    [BindProperty]
    public LoginInput Input { get; set; } = new();

    public bool Registered { get; private set; }
    public string NewUid { get; private set; } = string.Empty;

    public class LoginInput
    {
        [Required, Display(Name = "Email or Login")]
        public string Identifier { get; set; } = string.Empty;

        [Required, DataType(DataType.Password)]
        public string Password { get; set; } = string.Empty;
    }

    public void OnGet(string? registered, string? uid)
    {
        Registered = string.Equals(registered, "1", StringComparison.Ordinal);
        NewUid = uid ?? string.Empty;
    }

    public async Task<IActionResult> OnPostAsync()
    {
        if (!ModelState.IsValid)
        {
            return Page();
        }

        if (!_authService.ValidateCredentials(Input.Identifier, Input.Password, out var user) || user is null)
        {
            ModelState.AddModelError(string.Empty, "Incorrect login/email or password.");
            return Page();
        }

        var claims = new List<Claim>
        {
            new(ClaimTypes.Name, user.Login),
            new(ClaimTypes.Email, user.Email),
            new("uid", user.Uid)
        };

        var identity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
        var principal = new ClaimsPrincipal(identity);

        await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, principal);
        return RedirectToPage("/Dashboard");
    }
}