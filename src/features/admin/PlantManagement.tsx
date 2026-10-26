import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type DragEvent,
  type FormEvent,
} from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Pencil, Plus, RefreshCw, Save, X } from 'lucide-react';
import {
  archiveAdminPlant,
  createAdminPlant,
  getAdminPlants,
  getPlantCategories,
  setPlantPublished,
  updateAdminPlant,
  type AdminPlant,
  type PlantFormData,
} from '../../api/adminPlantApi';
import { uploadPlantImage } from '../../api/imageUploadApi';
import type { PlantCategory } from '../plants/plantTypes';

const emptyForm: PlantFormData = {
  categoryId: '',
  nameKn: '',
  nameEn: '',
  scientificName: '',
  descriptionKn: '',
  descriptionEn: '',
  slug: '',
  price: '',
  enquiryOnly: true,
  availability: 'UNKNOWN',
  published: false,
  featured: false,
  imageUrl: '',
};

export default function PlantManagement() {
  const [plants, setPlants] = useState<AdminPlant[]>([]);
  const [categories, setCategories] = useState<PlantCategory[]>([]);
  const [form, setForm] = useState<PlantFormData>(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [draggingImage, setDraggingImage] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const imageInputRef = useRef<HTMLInputElement | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const [plantData, categoryData] = await Promise.all([
        getAdminPlants(),
        getPlantCategories(),
      ]);

      setPlants(plantData);
      setCategories(categoryData);
    } catch {
      setError(
        'Could not load admin data. Confirm you are signed in and the backend is running.',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  function beginCreate() {
    setEditingId(null);
    setForm({
      ...emptyForm,
      categoryId: categories[0]?.id ?? '',
    });
    setNotice('');
    setError('');
    setShowForm(true);
  }

  function beginEdit(plant: AdminPlant) {
    setEditingId(plant.id);
    setForm({
      categoryId: plant.categoryId,
      nameKn: plant.nameKn,
      nameEn: plant.nameEn,
      scientificName: plant.scientificName ?? '',
      descriptionKn: plant.descriptionKn ?? '',
      descriptionEn: plant.descriptionEn ?? '',
      slug: plant.slug,
      price: plant.price == null ? '' : String(plant.price),
      enquiryOnly: plant.enquiryOnly,
      availability: plant.availability,
      published: plant.published,
      featured: plant.featured,
      imageUrl: plant.imageUrl ?? '',
    });
    setNotice('');
    setError('');
    setShowForm(true);
  }

  function updateField<K extends keyof PlantFormData>(
    field: K,
    value: PlantFormData[K],
  ) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function uploadImage(file: File) {
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      setError('Choose a JPEG or PNG image.');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError('Image must be 5 MB or smaller.');
      return;
    }

    setUploadingImage(true);
    setError('');
    setNotice('');

    try {
      const uploaded = await uploadPlantImage(file);
      setForm((current) => ({ ...current, imageUrl: uploaded.url }));
      setNotice('Image uploaded. Save the plant to keep its image.');
    } catch {
      setError('Image upload failed. Check your admin session and server.');
    } finally {
      setUploadingImage(false);
    }
  }

  async function handleImageUpload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];

    if (!file) return;

    await uploadImage(file);
    event.target.value = '';
  }

  function handleImageDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDraggingImage(false);

    if (uploadingImage || saving) return;

    const file = event.dataTransfer.files?.[0];

    if (!file) return;

    void uploadImage(file);
  }

  function clearImage() {
    setForm((current) => ({ ...current, imageUrl: '' }));
    setNotice('');
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');

    if (!form.categoryId) {
      setError('Select a category before saving.');
      setSaving(false);
      return;
    }

    if (!form.enquiryOnly && form.price.trim() === '') {
      setError('Enter a price or enable enquiry-only pricing.');
      setSaving(false);
      return;
    }

    if (
      form.price.trim() !== '' &&
      (!Number.isFinite(Number(form.price)) || Number(form.price) < 0)
    ) {
      setError('Price must be a valid non-negative number.');
      setSaving(false);
      return;
    }

    try {
      if (editingId) {
        await updateAdminPlant(editingId, form);
        setNotice('Plant updated successfully.');
      } else {
        await createAdminPlant(form);
        setNotice('Plant created successfully.');
      }

      setShowForm(false);
      setEditingId(null);
      setForm(emptyForm);
      await loadData();
    } catch (err: unknown) {
      const status =
        typeof err === 'object' && err !== null && 'response' in err
          ? (err as { response?: { status?: number } }).response?.status
          : undefined;

      setError(
        status === 401
          ? 'Your session has expired. Please sign in again.'
          : status === 403
            ? 'Access denied. Refresh your security session and try again.'
            : status === 409
              ? 'This slug is already used by another plant.'
              : 'Could not save the plant. Check the fields and backend logs.',
      );
    } finally {
      setSaving(false);
    }
  }

  async function handlePublish(plant: AdminPlant) {
    setError('');
    setNotice('');

    try {
      await setPlantPublished(plant.id, !plant.published);
      setNotice(
        plant.published
          ? 'Plant unpublished.'
          : 'Plant published.',
      );
      await loadData();
    } catch {
      setError('Could not change publishing status.');
    }
  }

  async function handleArchive(plant: AdminPlant) {
    const confirmed = window.confirm(
      `Archive "${plant.nameEn}"? It will no longer appear in the public catalogue.`,
    );

    if (!confirmed) return;

    setError('');
    setNotice('');

    try {
      await archiveAdminPlant(plant.id);
      setNotice('Plant archived.');
      await loadData();
    } catch {
      setError('Could not archive this plant.');
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
            <h1>Plant management</h1>
            <p>Add plants, update prices, and manage publication.</p>
          </div>

          <button
            type="button"
            className="plant-management-primary"
            onClick={beginCreate}
            disabled={categories.length === 0}
          >
            <Plus size={18} />
            Add plant
          </button>
        </header>

        {notice && (
          <p className="plant-management-success" role="status">
            {notice}
          </p>
        )}

        {error && (
          <p className="plant-management-error" role="alert">
            {error}
          </p>
        )}

        {showForm && (
          <section className="plant-editor">
            <div className="plant-editor-heading">
              <h2>{editingId ? 'Edit plant' : 'Add a new plant'}</h2>
              <button
                type="button"
                className="plant-editor-close"
                aria-label="Close form"
                onClick={() => setShowForm(false)}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="plant-editor-form">
              <label>
                Plant name in Kannada *
                <input
                  required
                  maxLength={200}
                  value={form.nameKn}
                  onChange={(e) => updateField('nameKn', e.target.value)}
                />
              </label>

              <label>
                Plant name in English *
                <input
                  required
                  maxLength={200}
                  value={form.nameEn}
                  onChange={(e) => updateField('nameEn', e.target.value)}
                />
              </label>

              <label>
                Category *
                <select
                  required
                  value={form.categoryId}
                  onChange={(e) => updateField('categoryId', e.target.value)}
                >
                  <option value="">Select category</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.nameKn} — {category.nameEn}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                URL slug (optional)
                <input
                  maxLength={220}
                  value={form.slug}
                  placeholder="grafted-mango"
                  onChange={(e) => updateField('slug', e.target.value)}
                />
              </label>

              <label>
                Scientific name
                <input
                  maxLength={250}
                  value={form.scientificName}
                  onChange={(e) => updateField('scientificName', e.target.value)}
                />
              </label>

              <label>
                Price in ₹ (leave empty if not set)
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.price}
                  onChange={(e) => updateField('price', e.target.value)}
                />
              </label>

              <label className="plant-editor-full">
                Description in Kannada
                <textarea
                  rows={4}
                  maxLength={5000}
                  value={form.descriptionKn}
                  onChange={(e) => updateField('descriptionKn', e.target.value)}
                />
              </label>

              <label className="plant-editor-full">
                Description in English
                <textarea
                  rows={4}
                  maxLength={5000}
                  value={form.descriptionEn}
                  onChange={(e) => updateField('descriptionEn', e.target.value)}
                />
              </label>

              <div className="plant-editor-full">
                <span className="plant-image-label">
                  Plant image (JPEG or PNG, maximum 5 MB)
                </span>

                <div
                  className={`plant-dropzone${draggingImage ? ' dragging' : ''}`}
                  onDragOver={(event) => {
                    event.preventDefault();
                    if (!uploadingImage && !saving) setDraggingImage(true);
                  }}
                  onDragLeave={() => setDraggingImage(false)}
                  onDrop={handleImageDrop}
                >
                  <input
                    ref={imageInputRef}
                    type="file"
                    accept="image/jpeg,image/png"
                    onChange={handleImageUpload}
                    disabled={uploadingImage || saving}
                    hidden
                  />

                  {form.imageUrl && (
                    <div className="plant-image-preview">
                      <img
                        src={form.imageUrl}
                        alt="Plant preview"
                        onError={(e) => {
                          e.currentTarget.style.display = 'none';
                        }}
                      />
                      <button
                        type="button"
                        className="plant-image-remove"
                        onClick={clearImage}
                        disabled={uploadingImage || saving}
                      >
                        <X size={15} />
                        Remove
                      </button>
                    </div>
                  )}

                  <button
                    type="button"
                    className="plant-dropzone-browse"
                    onClick={() => imageInputRef.current?.click()}
                    disabled={uploadingImage || saving}
                  >
                    {uploadingImage
                      ? 'Uploading image…'
                      : form.imageUrl
                        ? 'Drop a new image here or click to replace'
                        : 'Drag and drop an image here, or click to browse'}
                  </button>

                  <span className="plant-upload-help">
                    The uploaded image is stored by the backend and saved with
                    the plant when you press “Save plant”.
                  </span>
                </div>
              </div>

              <label>
                Availability
                <select
                  value={form.availability}
                  onChange={(e) =>
                    updateField(
                      'availability',
                      e.target.value as PlantFormData['availability'],
                    )
                  }
                >
                  <option value="UNKNOWN">Unknown — confirm with nursery</option>
                  <option value="AVAILABLE">Available</option>
                  <option value="OUT_OF_STOCK">Out of stock</option>
                </select>
              </label>

              <div className="plant-editor-options">
                <label className="plant-editor-checkbox">
                  <input
                    type="checkbox"
                    checked={form.enquiryOnly}
                    onChange={(e) => updateField('enquiryOnly', e.target.checked)}
                  />
                  Price on enquiry
                </label>

                <label className="plant-editor-checkbox">
                  <input
                    type="checkbox"
                    checked={form.featured}
                    onChange={(e) => updateField('featured', e.target.checked)}
                  />
                  Featured plant
                </label>

                <label className="plant-editor-checkbox">
                  <input
                    type="checkbox"
                    checked={form.published}
                    onChange={(e) => updateField('published', e.target.checked)}
                  />
                  Publish immediately
                </label>
              </div>

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
                  {saving ? 'Saving…' : 'Save plant'}
                </button>
              </div>
            </form>
          </section>
        )}

        <section className="plant-management-list">
          <div className="plant-list-heading">
            <h2>Plant catalogue</h2>
            <button
              type="button"
              className="plant-management-secondary"
              onClick={() => void loadData()}
              disabled={loading}
            >
              <RefreshCw size={16} />
              Refresh
            </button>
          </div>

          {loading ? (
            <p className="plant-management-empty" role="status">
              Loading plants…
            </p>
          ) : plants.length === 0 ? (
            <p className="plant-management-empty">
              No plants found. Add your first plant above.
            </p>
          ) : (
            <div className="plant-admin-table-wrap">
              <table className="plant-admin-table">
                <thead>
                  <tr>
                    <th>Plant</th>
                    <th>Price</th>
                    <th>Availability</th>
                    <th>Visibility</th>
                    <th>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {plants.map((plant) => (
                    <tr key={plant.id}>
                      <td>
                        <div className="plant-admin-name">
                          {plant.imageUrl ? (
                            <img src={plant.imageUrl} alt="" loading="lazy" />
                          ) : (
                            <span className="plant-admin-no-image">🌱</span>
                          )}
                          <div>
                            <strong>{plant.nameKn}</strong>
                            <span>{plant.nameEn}</span>
                            <small>{plant.slug}</small>
                          </div>
                        </div>
                      </td>

                      <td>
                        {plant.price == null
                          ? 'On enquiry'
                          : `₹${plant.price.toLocaleString('en-IN')}`}
                      </td>

                      <td>{plant.availability}</td>

                      <td>
                        <span
                          className={`plant-admin-status ${
                            plant.archived
                              ? 'archived'
                              : plant.published
                                ? 'published'
                                : 'draft'
                          }`}
                        >
                          {plant.archived
                            ? 'Archived'
                            : plant.published
                              ? 'Published'
                              : 'Draft'}
                        </span>
                      </td>

                      <td>
                        <div className="plant-admin-actions">
                          <button
                            type="button"
                            onClick={() => beginEdit(plant)}
                            disabled={plant.archived}
                            aria-label={`Edit ${plant.nameEn}`}
                          >
                            <Pencil size={16} />
                          </button>

                          <button
                            type="button"
                            onClick={() => void handlePublish(plant)}
                            disabled={plant.archived}
                          >
                            {plant.published ? 'Unpublish' : 'Publish'}
                          </button>

                          <button
                            type="button"
                            className="plant-admin-archive"
                            onClick={() => void handleArchive(plant)}
                            disabled={plant.archived}
                          >
                            Archive
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
