import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Pencil, Plus, Save } from 'lucide-react';
import {
  createCategory,
  getAdminCategories,
  setCategoryPublished,
  updateCategory,
  type AdminCategory,
  type CategoryFormData,
} from '../../api/adminCategoryApi';

const emptyForm: CategoryFormData = {
  nameKn: '',
  nameEn: '',
  slug: '',
  descriptionKn: '',
  descriptionEn: '',
  published: true,
};

export default function CategoryManagement() {
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [form, setForm] = useState<CategoryFormData>(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const refresh = useCallback(async () => {
    try {
      setCategories(await getAdminCategories());
      setError('');
    } catch {
      setError('Could not load categories. Check your admin session.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;

    getAdminCategories()
      .then((data) => {
        if (!active) return;
        setCategories(data);
        setError('');
      })
      .catch(() => {
        if (active) {
          setError('Could not load categories. Check your admin session.');
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  function change<K extends keyof CategoryFormData>(
    key: K,
    value: CategoryFormData[K],
  ) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function startCreate() {
    setEditingId(null);
    setForm(emptyForm);
    setShowForm(true);
    setError('');
    setNotice('');
  }

  function startEdit(category: AdminCategory) {
    setEditingId(category.id);
    setForm({
      nameKn: category.nameKn,
      nameEn: category.nameEn,
      slug: category.slug,
      descriptionKn: category.descriptionKn ?? '',
      descriptionEn: category.descriptionEn ?? '',
      published: category.published,
    });
    setShowForm(true);
    setError('');
    setNotice('');
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');

    try {
      if (editingId) {
        await updateCategory(editingId, form);
        setNotice('Category updated.');
      } else {
        await createCategory(form);
        setNotice('Category created.');
      }

      setShowForm(false);
      setEditingId(null);
      setForm(emptyForm);
      await refresh();
    } catch (err: unknown) {
      const status =
        typeof err === 'object' && err !== null && 'response' in err
          ? (err as { response?: { status?: number } }).response?.status
          : undefined;

      setError(
        status === 409
          ? 'That category slug is already in use.'
          : 'Could not save the category. Check the required fields.',
      );
    } finally {
      setSaving(false);
    }
  }

  async function togglePublished(category: AdminCategory) {
    setError('');
    setNotice('');

    try {
      await setCategoryPublished(category.id, !category.published);
      setNotice(category.published ? 'Category hidden.' : 'Category published.');
      await refresh();
    } catch {
      setError('Could not update category publication status.');
    }
  }

  return (
    <main className="plant-management-page">
      <div className="plant-management-container">
        <Link to="/admin/dashboard" className="plant-management-back">
          <ArrowLeft size={18} />
          Back to dashboard
        </Link>

        <header className="plant-management-header">
          <div>
            <p className="plant-management-eyebrow">ADMINISTRATION</p>
            <h1>Category management</h1>
            <p>Organize the nursery catalogue in Kannada and English.</p>
          </div>

          <button
            type="button"
            className="plant-management-primary"
            onClick={startCreate}
          >
            <Plus size={18} />
            Add category
          </button>
        </header>

        {notice && (
          <p className="plant-management-success" role="status">{notice}</p>
        )}

        {error && (
          <p className="plant-management-error" role="alert">{error}</p>
        )}

        {showForm && (
          <section className="plant-editor">
            <h2>{editingId ? 'Edit category' : 'Create category'}</h2>

            <form className="plant-editor-form" onSubmit={submit}>
              <label>
                Kannada name *
                <input
                  required
                  maxLength={150}
                  value={form.nameKn}
                  onChange={(e) => change('nameKn', e.target.value)}
                />
              </label>

              <label>
                English name *
                <input
                  required
                  maxLength={150}
                  value={form.nameEn}
                  onChange={(e) => change('nameEn', e.target.value)}
                />
              </label>

              <label className="plant-editor-full">
                URL slug
                <input
                  maxLength={180}
                  placeholder="fruit-plants"
                  value={form.slug}
                  onChange={(e) => change('slug', e.target.value)}
                />
              </label>

              <label className="plant-editor-full">
                Kannada description
                <textarea
                  rows={3}
                  maxLength={5000}
                  value={form.descriptionKn}
                  onChange={(e) => change('descriptionKn', e.target.value)}
                />
              </label>

              <label className="plant-editor-full">
                English description
                <textarea
                  rows={3}
                  maxLength={5000}
                  value={form.descriptionEn}
                  onChange={(e) => change('descriptionEn', e.target.value)}
                />
              </label>

              <label className="plant-editor-checkbox plant-editor-full">
                <input
                  type="checkbox"
                  checked={form.published}
                  onChange={(e) => change('published', e.target.checked)}
                />
                Publish category
              </label>

              <div className="plant-editor-actions plant-editor-full">
                <button
                  type="button"
                  className="plant-management-secondary"
                  onClick={() => setShowForm(false)}
                  disabled={saving}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="plant-management-primary"
                  disabled={saving}
                >
                  <Save size={17} />
                  {saving ? 'Saving…' : 'Save category'}
                </button>
              </div>
            </form>
          </section>
        )}

        <section className="plant-management-list">
          <h2>All categories</h2>

          {loading ? (
            <p className="plant-management-empty">Loading categories…</p>
          ) : categories.length === 0 ? (
            <p className="plant-management-empty">
              No categories found. Create the first category.
            </p>
          ) : (
            <div className="plant-admin-table-wrap">
              <table className="plant-admin-table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Slug</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {categories.map((category) => (
                    <tr key={category.id}>
                      <td>
                        <strong>{category.nameKn}</strong>
                        <br />
                        <span>{category.nameEn}</span>
                      </td>
                      <td>{category.slug}</td>
                      <td>
                        <span
                          className={`plant-admin-status ${
                            category.published ? 'published' : 'draft'
                          }`}
                        >
                          {category.published ? 'Published' : 'Hidden'}
                        </span>
                      </td>
                      <td>
                        <div className="plant-admin-actions">
                          <button
                            type="button"
                            onClick={() => startEdit(category)}
                          >
                            <Pencil size={15} />
                            Edit
                          </button>
                          <button
                            type="button"
                            onClick={() => void togglePublished(category)}
                          >
                            {category.published ? 'Hide' : 'Publish'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
