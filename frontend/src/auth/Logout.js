/**
 * Performs a full logout by clearing all stored authentication data
 * from the browser and redirecting the user to the login page.
 *
 * This function is used when:
 * - The JWT token has expired or is invalid
 * - The user logs out manually (if triggered from a logout button)
 * - We need to ensure that all authentication state is completely reset
 */

export const hardLogout = () => {
  // Clear local and session storage
  localStorage.clear();
  sessionStorage.clear();

  // Remove cookies
  document.cookie.split(';').forEach((c) => {
    document.cookie = c
      .replace(/^ +/, '')
      .replace(/=.*/, '=;expires=' + new Date().toUTCString() + ';path=/');
  });

  // Redirect to login page
  window.location.href = '/login';
};
