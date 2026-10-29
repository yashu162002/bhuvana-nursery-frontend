import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { MailCheck } from 'lucide-react';
import { apiErrorMessage, requestPasswordReset } from '../../api/authApi';

/**
 * Step 1 of the password reset flow: the administrator asks for a link by
 * submitting the configured admin Gmail address. The backend answers with the
 * same generic message whether or not the address exists, so this page never
 * reveals whether an account matches.
 */
export default function AdminForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sentMessage, setSentMessage] = useState('');
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmed = email.trim();
    if (!trimmed) {
      setError('Please enter your administrator email address.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      const message = await requestPasswordReset(trimmed);
      setSentMessage(message);
    } catch (err: unknown) {
      setError(
        apiErrorMessage(
          err,
          'Unable to send the reset request. Check the server connection and try again.',
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
          <MailCheck size={26} />
        </div>

        <h1>Forgot your password?</h1>
        <p className="admin-login-subtitle">
          Enter the administrator email address and we will send a secure link to
          choose a new password.
        </p>

        {sentMessage ? (
          <>
            <p className="admin-login-notice" role="status">
              {sentMessage}
            </p>
            <Link to="/admin/login" className="admin-back-home">
              ← Back to login
            </Link>
          </>
        ) : (
          <form onSubmit={handleSubmit} className="admin-login-form">
            <label htmlFor="forgot-email">Administrator email</label>
            <input
              id="forgot-email"
              type="email"
              autoComplete="email"
              placeholder="you@gmail.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              maxLength={320}
            />

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
              {loading ? 'Sending reset link…' : 'Send reset link'}
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
