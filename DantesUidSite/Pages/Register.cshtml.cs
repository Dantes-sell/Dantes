using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using DantesUidSite.Services;

namespace DantesUidSite.Pages;

public class RegisterModel : PageModel
{
    private readonly UserAuthService _authService;

    public RegisterModel(UserAuthService authService)
    {
        _authService = authService;
    }

    [BindProperty]
    public RegisterInput Input { get; set; } = new();

    public class RegisterInput
    {
        [Required, EmailAddress]
        public string Email { get; set; } = string.Empty;

        [Required, MinLength(3), MaxLength(24)]
        public string Login { get; set; } = string.Empty;

        [Required, MinLength(6), DataType(DataType.Password)]
        public string Password { get; set; } = string.Empty;
    }

    public IActionResult OnPost()
    {
        if (!ModelState.IsValid)
        {
            return Page();
        }

        if (!_authService.Register(Input.Email, Input.Login, Input.Password, out var error, out var uid))
        {
            ModelState.AddModelError(string.Empty, error);
            return Page();
        }

        return RedirectToPage("/Login", new { registered = "1", uid });
    }
}