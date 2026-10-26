import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Images, LogOut, ShieldCheck, Sprout } from 'lucide-react';
import { getCurrentAdmin, logout } from '../../api/authApi';
import type { AdminUser } from '../../api/authApi';
import { getAdminPlants } from '../../api/adminPlantApi';
import { getAdminCategories } from '../../api/adminCategoryApi';
import { getAdminHeroSlides } from '../../api/adminHeroSlideApi';

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [admin, setAdmin] = useState<AdminUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [stats, setStats] = useState({
    plants: 0,
    publishedPlants: 0,
    draftPlants: 0,
    categories: 0,
    heroSlides: 0,
    activeHeroSlides: 0,
  });

  useEffect(() => {
    let cancelled = false;

    getCurrentAdmin()
      .then((user) => {
        if (cancelled) return;

        if (!user) {
          navigate('/admin/login', { replace: true });
          return;
        }

        setAdmin(user);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [navigate]);

  useEffect(() => {
    let cancelled = false;

    Promise.all([
      getAdminPlants(),
      getAdminCategories(),
      getAdminHeroSlides().catch(() => []),
    ])
      .then(([plants, categories, slides]) => {
        if (cancelled) return;

        const activePlants = plants.filter((plant) => !plant.archived);

        setStats({
          plants: activePlants.length,
          publishedPlants: activePlants.filter((plant) => plant.published).length,
          draftPlants: activePlants.filter((plant) => !plant.published).length,
          categories: categories.length,
          heroSlides: slides.length,
          activeHeroSlides: slides.filter((s) => s.active).length,
        });
      })
      .catch(() => {
        // The dashboard remains usable even if statistics fail to load.
      });

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleLogout() {
    setError('');

    try {
      await logout();
      navigate('/admin/login', { replace: true });
    } catch {
      setError('Could not sign out. Please try again.');
    }
  }

  if (loading || !admin) {
    return (
      <main className="admin-dashboard-loading" role="status">
        Checking administrator session…
      </main>
    );
  }

  return (
    <main className="admin-dashboard">
      <header className="admin-dashboard-header">
        <Link to="/" className="admin-brand">
          <img src="/logo.png" alt="Bhoovana Nursery Logo" className="admin-brand-logo" />
          <span>
            <strong>ಭೂವನ ನರ್ಸರಿ</strong>
            <small>Admin Console</small>
          </span>
        </Link>

        <button
          type="button"
          className="admin-logout-button"
          onClick={handleLogout}
        >
          <LogOut size={17} />
          Sign out
        </button>
      </header>

      <div className="admin-dashboard-content">
        <div className="admin-welcome">
          <div>
            <span className="admin-overline">ADMINISTRATION</span>
            <h1>Welcome, {admin.username}</h1>
            <p>Manage the Bhoovana Nursery website.</p>
          </div>

          <span className="admin-role">
            <ShieldCheck size={17} />
            Administrator
          </span>
        </div>

        {error && (
          <p className="admin-login-error" role="alert">
            {error}
          </p>
        )}

        <section className="admin-stats-grid" aria-label="Catalogue statistics">
          <article className="admin-stat-card">
            <span>Hero slideshow</span>
            <strong>
              {stats.activeHeroSlides} / {stats.heroSlides || 6}
            </strong>
          </article>

          <article className="admin-stat-card">
            <span>Total active plants</span>
            <strong>{stats.plants}</strong>
          </article>

          <article className="admin-stat-card">
            <span>Published plants</span>
            <strong>{stats.publishedPlants}</strong>
          </article>

          <article className="admin-stat-card">
            <span>Categories</span>
            <strong>{stats.categories}</strong>
          </article>
        </section>

        <section className="admin-dashboard-grid">
          <article className="admin-dashboard-card">
            <Images size={28} />
            <h2>Hero slideshow</h2>
            <p>
              Upload and manage the 6 continuous slideshow images featured in the homepage hero section.
            </p>
            <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
              <Link to="/admin/hero-slides">Manage hero slides →</Link>
              <Link to="/" target="_blank" rel="noreferrer">
                Preview live hero
              </Link>
            </div>
          </article>

          <article className="admin-dashboard-card">
            <Sprout size={28} />
            <h2>Plant catalogue</h2>
            <p>
              Manage plants, prices, availability status, and plant images.
            </p>
            <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
              <Link to="/admin/plants">Manage plants →</Link>
              <Link to="/#plants">View public catalogue</Link>
            </div>
          </article>

          <article className="admin-dashboard-card">
            <ShieldCheck size={28} />
            <h2>Account security</h2>
            <p>
              You are signed in using a server-verified administrator session.
            </p>
            <span className="admin-security-status">Session active</span>
          </article>
        </section>

        <section className="admin-quick-actions">
          <h2>Quick actions</h2>

          <div>
            <Link to="/admin/hero-slides">Manage hero slideshow →</Link>
            <Link to="/admin/plants">Manage plants →</Link>
            <Link to="/admin/categories">Manage categories →</Link>
            <Link to="/#plants">View public catalogue →</Link>
          </div>
        </section>
      </div>
    </main>
  );
}
