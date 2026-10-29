import { useEffect, useState } from 'react';
import {
  BrowserRouter,
  Link,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom';
import App from './App';
import PlantDetailPage from './features/plants/PlantDetailPage';
import AdminLoginPage from './features/admin/AdminLoginPage';
import AdminForgotPasswordPage from './features/admin/AdminForgotPasswordPage';
import AdminResetPasswordPage from './features/admin/AdminResetPasswordPage';
import AdminDashboard from './features/admin/AdminDashboard';
import PlantManagement from './features/admin/PlantManagement';
import CategoryManagement from './features/admin/CategoryManagement';
import HeroSlideManagement from './features/admin/HeroSlideManagement';
import { getCurrentAdmin } from './api/authApi';
import LanguageProvider from './context/LanguageProvider';
import { useLanguage } from './context/languageContext';
import PublicLayout from './components/luxury/PublicLayout';

function PlantDetailRoute() {
  const { isKannada } = useLanguage();
  return <PlantDetailPage kannada={isKannada} />;
}

function AdminGuard({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [checking, setChecking] = useState(true);
  const [authorized, setAuthorized] = useState(false);

  useEffect(() => {
    let cancelled = false;

    getCurrentAdmin()
      .then((user) => {
        if (cancelled) return;

        if (user?.role === 'ADMIN') {
          setAuthorized(true);
        } else {
          navigate('/admin/login', {
            replace: true,
            state: { from: location.pathname },
          });
        }
      })
      .finally(() => {
        if (!cancelled) setChecking(false);
      });

    return () => {
      cancelled = true;
    };
  }, [location.pathname, navigate]);

  if (checking || !authorized) {
    return (
      <main className="admin-dashboard-loading" role="status">
        Verifying administrator access…
      </main>
    );
  }

  return children;
}

function NotFound() {
  return (
    <main className="plant-detail-state">
      <h1>404</h1>
      <p>Page not found / ಪುಟ ಕಂಡುಬಂದಿಲ್ಲ</p>
      <Link to="/">Return home / ಮುಖಪುಟಕ್ಕೆ</Link>
    </main>
  );
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <LanguageProvider>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route path="/" element={<App />} />
          <Route path="/plants/:slug" element={<PlantDetailRoute />} />
        </Route>
        <Route path="/admin/login" element={<AdminLoginPage />} />
        <Route
          path="/admin/forgot-password"
          element={<AdminForgotPasswordPage />}
        />
        <Route
          path="/admin/reset-password"
          element={<AdminResetPasswordPage />}
        />
        <Route
          path="/admin/dashboard"
          element={
            <AdminGuard>
              <AdminDashboard />
            </AdminGuard>
          }
        />
        <Route
          path="/admin/plants"
          element={
            <AdminGuard>
              <PlantManagement />
            </AdminGuard>
          }
        />
        <Route
          path="/admin/categories"
          element={
            <AdminGuard>
              <CategoryManagement />
            </AdminGuard>
          }
        />
        <Route
          path="/admin/hero-slides"
          element={
            <AdminGuard>
              <HeroSlideManagement />
            </AdminGuard>
          }
        />
        <Route path="*" element={<NotFound />} />
      </Routes>
      </LanguageProvider>
    </BrowserRouter>
  );
}
