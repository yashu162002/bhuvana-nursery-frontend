import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { KeyRound, Eye, EyeOff } from 'lucide-react';
import { apiErrorMessage, resetPassword } from '../../api/authApi';

/** Mirrors PasswordPolicy on the backend: 10+ chars, at least one letter and one digit. */
const PASSWORD_RULES =
  'Password must be at least 10 characters and include both a letter and a number.';

function passwordError(value: string): string {
  if (value.length < 10 || value.length > 200) return PASSWORD_RULES;
  if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) return PASSWORD_RULES;
  return '';
}

/**
 * Step 2 of the password reset flow. The single-use token arrives in the
 * query string of the emailed link and is redeemed against the backend, which
 * stores the new BCrypt hash in PostgreSQL so it survives restarts.
 */
export default function AdminResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');

  const linkMissing = token.trim().length === 0;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (linkMissing) {
      setError('This reset link is invalid or incomplete. Please request a new one.');
      return;
    }

    const policyError = passwordError(password);
    if (policyError) {
      setError(policyError);
      return;
    }

    if (password !== confirm) {
      setError('Passwords do not match.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      await resetPassword(token.trim(), password);
      setDone(true);
      setPassword('');
      setConfirm('');
    } catch (err: unknown) {
      setError(
        apiErrorMessage(
          err,
          'Unable to reset the password. The link may have expired — request a new one.',
        ),
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="admin-login-page">
      <section className="admin-login-card">
        <Link to="/" className="admin-brand">
          <img src="/logo.png" alt="Bhoovana Nursery Logo" className="admin-brand-logo" />
          <span>
            <strong>ಭೂವನ ನರ್ಸರಿ</strong>
            <small>Bhoovana Nursery</small>
          </span>
        </Link>

        <div className="admin-login-icon">
          <KeyRound size={26} />
        </div>

        <h1>Choose a new password</h1>
        <p className="admin-login-subtitle">
          This link can be used once and expires shortly after it is sent.
        </p>

        {done ? (
          <>
            <p className="admin-login-notice" role="status">
              Password updated. You can now sign in with your new password.
            </p>
            <Link to="/admin/login" className="admin-back-home">
              ← Go to login
            </Link>
          </>
        ) : linkMissing ? (
          <>
            <p className="admin-login-error" role="alert">
              This reset link is invalid or incomplete. Please request a new one.
            </p>
            <Link to="/admin/forgot-password" className="admin-back-home">
              Request a new link
            </Link>
          </>
        ) : (
          <form onSubmit={handleSubmit} className="admin-login-form">
            <label htmlFor="reset-password">New password</label>
            <div className="admin-password-field">
              <input
                id="reset-password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                maxLength={200}
              />
              <button
                type="button"
                className="admin-password-toggle"
                onClick={() => setShowPassword((value) => !value)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={19} /> : <Eye size={19} />}
              </button>
            </div>

            <label htmlFor="reset-confirm">Confirm new password</label>
            <input
              id="reset-confirm"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              value={confirm}
              onChange={(event) => setConfirm(event.target.value)}
              required
              maxLength={200}
            />

            <p className="admin-login-hint">{PASSWORD_RULES}</p>

            {error && (
              <p className="admin-login-error" role="alert">
                {error}
              </p>
            )}

            <button
              className="admin-login-submit"
              type="submit"
              disabled={loading}
            >
              {loading ? 'Updating password…' : 'Update password'}
            </button>
          </form>
        )}

        <p className="admin-login-links">
          <Link to="/admin/login" className="admin-forgot-link">
            ← Back to login
          </Link>
        </p>
      </section>
    </main>
  );
}
