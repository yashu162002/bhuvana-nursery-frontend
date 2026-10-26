import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { LockKeyhole, Eye, EyeOff } from 'lucide-react';
import { login } from '../../api/authApi';

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(username.trim(), password);
      navigate('/admin/dashboard', { replace: true });
    } catch (err: unknown) {
      const status =
        typeof err === 'object' && err !== null && 'response' in err
          ? (err as { response?: { status?: number } }).response?.status
          : undefined;

      setError(
        status === 401
          ? 'Invalid username or password.'
          : status === 403
            ? 'Security token expired. Please refresh and try again.'
            : 'Unable to sign in. Check the server connection and try again.',
      );
    } finally {
      setLoading(false);
      setPassword('');
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
          <LockKeyhole size={26} />
        </div>

        <h1>Administrator Login</h1>
        <p className="admin-login-subtitle">
          Sign in to manage the nursery website.
        </p>

        <form onSubmit={handleSubmit} className="admin-login-form">
          <label htmlFor="admin-username">Username</label>
          <input
            id="admin-username"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
            maxLength={100}
          />

          <label htmlFor="admin-password">Password</label>
          <div className="admin-password-field">
            <input
              id="admin-password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
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
            {loading ? 'Signing in…' : 'Sign in securely'}
          </button>
        </form>

        <Link to="/" className="admin-back-home">
          ← Back to website
        </Link>
      </section>
    </main>
  );
}
