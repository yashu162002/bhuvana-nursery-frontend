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
import {
  ArrowDown,
  ArrowLeft,
  ArrowUp,
  Check,
  ExternalLink,
  Eye,
  EyeOff,
  Pencil,
  Plus,
  RefreshCw,
  RotateCcw,
  Save,
  Trash2,
  UploadCloud,
  X,
} from 'lucide-react';
import {
  createAdminHeroSlide,
  deleteAdminHeroSlide,
  getAdminHeroSlides,
  reorderAdminHeroSlides,
  resetDefaultHeroSlides,
  toggleAdminHeroSlideActive,
  updateAdminHeroSlide,
  type AdminHeroSlide,
  type HeroSlideFormData,
} from '../../api/adminHeroSlideApi';
import { uploadPlantImage } from '../../api/imageUploadApi';
import { assetUrl } from '../../config/siteConfig';

const emptyForm: HeroSlideFormData = {
  imageUrl: '',
  titleKn: '',
  titleEn: '',
  altText: '',
  displayOrder: 1,
  active: true,
};

export default function HeroSlideManagement() {
  const [slides, setSlides] = useState<AdminHeroSlide[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingSlotId, setUploadingSlotId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  // Edit / Create Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editingSlide, setEditingSlide] = useState<AdminHeroSlide | null>(null);
  const [form, setForm] = useState<HeroSlideFormData>(emptyForm);
  const [modalUploading, setModalUploading] = useState(false);

  // Hidden file inputs
  const slotFileInputRef = useRef<HTMLInputElement | null>(null);
  const targetSlotIdRef = useRef<string | null>(null);
  const modalFileInputRef = useRef<HTMLInputElement | null>(null);

  const loadData = useCallback(async () => {
    try {
      const data = await getAdminHeroSlides();
      setSlides(data);
      setError('');
    } catch {
      setError('Could not load hero slides. Please verify your admin session.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;

    getAdminHeroSlides()
      .then((data) => {
        if (!active) return;
        setSlides(data);
        setError('');
      })
      .catch(() => {
        if (active) {
          setError('Could not load hero slides. Please verify your admin session.');
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  // Quick replacement of photo for a specific slide slot
  function triggerSlotUpload(slideId: string) {
    targetSlotIdRef.current = slideId;
    slotFileInputRef.current?.click();
  }

  async function handleSlotFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    const slideId = targetSlotIdRef.current;
    event.target.value = '';

    if (!file || !slideId) return;

    const slide = slides.find((s) => s.id === slideId);
    if (!slide) return;

    if (file.size > 5 * 1024 * 1024) {
      setError('Image must be under 5 MB in size.');
      return;
    }

    setUploadingSlotId(slideId);
    setError('');
    setNotice('');

    try {
      const uploaded = await uploadPlantImage(file);
      await updateAdminHeroSlide(slide.id, {
        imageUrl: uploaded.url,
        titleKn: slide.titleKn ?? '',
        titleEn: slide.titleEn ?? '',
        altText: slide.altText ?? '',
        displayOrder: slide.displayOrder,
        active: slide.active,
      });

      setNotice(`Slide ${slide.displayOrder} photo replaced successfully.`);
      await loadData();
    } catch {
      setError('Failed to upload image. Please check file format and server connection.');
    } finally {
      setUploadingSlotId(null);
      targetSlotIdRef.current = null;
    }
  }

  // Drag and drop for a slide slot
  async function handleSlotDrop(event: DragEvent<HTMLDivElement>, slide: AdminHeroSlide) {
    event.preventDefault();
    if (uploadingSlotId || saving) return;

    const file = event.dataTransfer.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      setError('Image must be under 5 MB in size.');
      return;
    }

    setUploadingSlotId(slide.id);
    setError('');
    setNotice('');

    try {
      const uploaded = await uploadPlantImage(file);
      await updateAdminHeroSlide(slide.id, {
        imageUrl: uploaded.url,
        titleKn: slide.titleKn ?? '',
        titleEn: slide.titleEn ?? '',
        altText: slide.altText ?? '',
        displayOrder: slide.displayOrder,
        active: slide.active,
      });

      setNotice(`Slide ${slide.displayOrder} photo updated successfully.`);
      await loadData();
    } catch {
      setError('Failed to upload image. Only JPEG and PNG under 5 MB are accepted.');
    } finally {
      setUploadingSlotId(null);
    }
  }

  // Open Edit Details Modal
  function openEditModal(slide: AdminHeroSlide) {
    setEditingSlide(slide);
    setForm({
      imageUrl: slide.imageUrl,
      titleKn: slide.titleKn ?? '',
      titleEn: slide.titleEn ?? '',
      altText: slide.altText ?? '',
      displayOrder: slide.displayOrder,
      active: slide.active,
    });
    setError('');
    setNotice('');
    setModalOpen(true);
  }

  // Open Add Slide Modal
  function openCreateModal() {
    setEditingSlide(null);
    setForm({
      ...emptyForm,
      displayOrder: slides.length + 1,
    });
    setError('');
    setNotice('');
    setModalOpen(true);
  }

  // Save Modal Form
  async function handleModalSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!form.imageUrl.trim()) {
      setError('An image is required. Upload an image or enter a valid URL.');
      return;
    }

    setSaving(true);
    setError('');
    setNotice('');

    try {
      if (editingSlide) {
        await updateAdminHeroSlide(editingSlide.id, form);
        setNotice(`Slide ${form.displayOrder} details updated.`);
      } else {
        await createAdminHeroSlide(form);
        setNotice('New slide added to hero section.');
      }
      setModalOpen(false);
      setEditingSlide(null);
      await loadData();
    } catch {
      setError('Could not save slide. Check input fields and backend connection.');
    } finally {
      setSaving(false);
    }
  }

  // Modal Image Upload
  async function handleModalImageUpload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      setError('Image must be under 5 MB.');
      return;
    }

    setModalUploading(true);
    setError('');

    try {
      const uploaded = await uploadPlantImage(file);
      setForm((prev) => ({ ...prev, imageUrl: uploaded.url }));
    } catch {
      setError('Could not upload image. Ensure it is a valid JPEG or PNG file.');
    } finally {
      setModalUploading(false);
    }
  }

  // Toggle Active
  async function handleToggleActive(slide: AdminHeroSlide) {
    setError('');
    setNotice('');
    try {
      await toggleAdminHeroSlideActive(slide.id);
      setNotice(`Slide ${slide.displayOrder} is now ${slide.active ? 'hidden' : 'active'}.`);
      await loadData();
    } catch {
      setError('Could not change slide status.');
    }
  }

  // Reordering
  async function handleMove(index: number, direction: -1 | 1) {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= slides.length) return;

    setError('');
    setNotice('');

    const newSlides = [...slides];
    const [moved] = newSlides.splice(index, 1);
    newSlides.splice(targetIndex, 0, moved);

    const orderedIds = newSlides.map((s) => s.id);

    try {
      await reorderAdminHeroSlides(orderedIds);
      setNotice('Slide order updated.');
      await loadData();
    } catch {
      setError('Could not update slide order.');
    }
  }

  // Delete Slide
  async function handleDelete(slide: AdminHeroSlide) {
    const confirmed = window.confirm(
      `Delete Slide ${slide.displayOrder} (${slide.titleEn || 'Hero Slide'})? The remaining slides will re-index.`,
    );
    if (!confirmed) return;

    setError('');
    setNotice('');
    try {
      await deleteAdminHeroSlide(slide.id);
      setNotice(`Slide ${slide.displayOrder} deleted.`);
      await loadData();
    } catch {
      setError('Could not delete slide.');
    }
  }

  // Reset to default 6 slides
  async function handleResetDefaults() {
    const confirmed = window.confirm(
      'Restore the default 6 hero slides (The green canopy, New beginnings, Woods of light, Grown with care, The path to your garden, Botanical beauty)? This will replace all current hero slides.',
    );
    if (!confirmed) return;

    setSaving(true);
    setError('');
    setNotice('');

    try {
      const restored = await resetDefaultHeroSlides();
      setSlides(restored);
      setNotice('Reset successful: The default 6 hero slides have been restored.');
    } catch {
      setError('Could not reset slides to defaults.');
    } finally {
      setSaving(false);
    }
  }

  const activeCount = slides.filter((s) => s.active).length;

  return (
    <main className="plant-management-page">
      <div className="plant-management-container">
        {/* Hidden file input for single-click slot replacement */}
        <input
          ref={slotFileInputRef}
          type="file"
          accept="image/jpeg,image/png"
          onChange={handleSlotFileChange}
          style={{ display: 'none' }}
        />

        <Link to="/admin/dashboard" className="plant-management-back">
          <ArrowLeft size={18} />
          Back to dashboard
        </Link>

        <header className="plant-management-header">
          <div>
            <p className="plant-management-eyebrow">ADMINISTRATION</p>
            <h1>Hero slideshow</h1>
            <p>
              Upload and manage the 6 continuous slideshow images featured in the homepage hero section.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button
              type="button"
              className="plant-management-secondary"
              onClick={handleResetDefaults}
              disabled={saving || loading}
              title="Restore the initial 6 nursery hero photographs"
            >
              <RotateCcw size={16} />
              Reset to 6 defaults
            </button>

            <Link
              to="/"
              target="_blank"
              rel="noreferrer"
              className="plant-management-secondary"
              style={{ textDecoration: 'none' }}
              title="Preview the live hero section"
            >
              <ExternalLink size={16} />
              View hero live
            </Link>

            <button
              type="button"
              className="plant-management-primary"
              onClick={openCreateModal}
              disabled={saving || loading}
            >
              <Plus size={18} />
              Add slide
            </button>
          </div>
        </header>

        {notice && (
          <p className="plant-management-success" role="status">
            <Check size={18} style={{ display: 'inline', marginRight: '6px', verticalAlign: 'text-bottom' }} />
            {notice}
          </p>
        )}

        {error && (
          <p className="plant-management-error" role="alert">
            {error}
          </p>
        )}

        <div className="hero-status-bar">
          <div className="hero-status-info">
            <span className="hero-status-badge">
              {activeCount} of {slides.length} active in slideshow
            </span>
            <span className="hero-status-tip">
              Recommended: Landscape photos (at least 2000px wide, max 5 MB). The homepage continuously slides through all active images.
            </span>
          </div>

          <button
            type="button"
            className="hero-refresh-btn"
            onClick={() => void loadData()}
            disabled={loading}
            title="Refresh slides"
          >
            <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
            Refresh
          </button>
        </div>

        {loading ? (
          <div className="plant-management-list" style={{ textAlign: 'center', padding: '60px 20px' }}>
            <p>Loading slideshow images…</p>
          </div>
        ) : slides.length === 0 ? (
          <div className="plant-management-list" style={{ textAlign: 'center', padding: '60px 20px' }}>
            <p>No hero slides configured.</p>
            <button
              type="button"
              className="plant-management-primary"
              style={{ marginTop: '16px' }}
              onClick={handleResetDefaults}
            >
              <RotateCcw size={17} />
              Load default 6 slides
            </button>
          </div>
        ) : (
          <div className="hero-slides-grid">
            {slides.map((slide, index) => {
              const isUploadingThis = uploadingSlotId === slide.id;

              return (
                <div
                  key={slide.id}
                  className={`hero-slide-card${!slide.active ? ' is-inactive' : ''}`}
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => handleSlotDrop(e, slide)}
                >
                  <div className="hero-slide-header">
                    <span className="hero-slide-slot-badge">
                      Slot {index + 1}
                    </span>

                    <div className="hero-slide-header-actions">
                      <button
                        type="button"
                        className={`hero-slide-visibility-btn ${slide.active ? 'active' : 'inactive'}`}
                        onClick={() => handleToggleActive(slide)}
                        title={slide.active ? 'Currently active in slideshow (click to pause)' : 'Hidden (click to activate)'}
                      >
                        {slide.active ? (
                          <>
                            <Eye size={14} /> Active
                          </>
                        ) : (
                          <>
                            <EyeOff size={14} /> Hidden
                          </>
                        )}
                      </button>
                    </div>
                  </div>

                  {/* Image Preview & Replacement Box */}
                  <div className="hero-slide-image-box">
                    <img
                      src={assetUrl(slide.imageUrl) ?? slide.imageUrl}
                      alt={slide.altText || slide.titleEn || 'Hero slide'}
                      className="hero-slide-thumb"
                      onError={(e) => {
                        e.currentTarget.src =
                          'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=600&q=60';
                      }}
                    />

                    {isUploadingThis && (
                      <div className="hero-slide-upload-overlay">
                        <RefreshCw size={24} className="animate-spin" />
                        <span>Uploading…</span>
                      </div>
                    )}

                    <div className="hero-slide-hover-actions">
                      <button
                        type="button"
                        className="hero-slide-replace-btn"
                        onClick={() => triggerSlotUpload(slide.id)}
                        disabled={isUploadingThis || saving}
                      >
                        <UploadCloud size={16} />
                        Replace photo
                      </button>
                    </div>
                  </div>

                  {/* Caption & Titles */}
                  <div className="hero-slide-body">
                    <div className="hero-slide-titles">
                      <div className="hero-slide-title-kn">
                        <small>Kannada caption:</small>
                        <strong>{slide.titleKn || '—'}</strong>
                      </div>
                      <div className="hero-slide-title-en">
                        <small>English caption:</small>
                        <span>{slide.titleEn || '—'}</span>
                      </div>
                    </div>

                    <div className="hero-slide-meta">
                      <span className="hero-slide-url-preview" title={slide.imageUrl}>
                        {slide.imageUrl.startsWith('/uploads/')
                          ? 'Uploaded photo'
                          : 'Remote image URL'}
                      </span>
                    </div>
                  </div>

                  {/* Card Actions Footer */}
                  <div className="hero-slide-footer">
                    <div className="hero-slide-reorder-group">
                      <button
                        type="button"
                        className="hero-icon-btn"
                        disabled={index === 0 || saving}
                        onClick={() => handleMove(index, -1)}
                        title="Move left / up"
                      >
                        <ArrowUp size={15} />
                      </button>
                      <button
                        type="button"
                        className="hero-icon-btn"
                        disabled={index === slides.length - 1 || saving}
                        onClick={() => handleMove(index, 1)}
                        title="Move right / down"
                      >
                        <ArrowDown size={15} />
                      </button>
                    </div>

                    <div className="hero-slide-edit-group">
                      <button
                        type="button"
                        className="hero-icon-btn"
                        onClick={() => openEditModal(slide)}
                        title="Edit titles and details"
                      >
                        <Pencil size={15} />
                      </button>

                      <button
                        type="button"
                        className="hero-icon-btn danger"
                        onClick={() => handleDelete(slide)}
                        title="Delete slide"
                        disabled={saving}
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Modal for Adding or Editing Slide Details */}
        {modalOpen && (
          <div className="hero-modal-backdrop" onClick={() => setModalOpen(false)}>
            <div
              className="hero-modal-window"
              onClick={(e) => e.stopPropagation()}
              role="dialog"
              aria-modal="true"
            >
              <div className="hero-modal-header">
                <h2>{editingSlide ? `Edit Slide ${form.displayOrder}` : 'Add New Hero Slide'}</h2>
                <button
                  type="button"
                  className="hero-modal-close"
                  onClick={() => setModalOpen(false)}
                >
                  <X size={20} />
                </button>
              </div>

              <form onSubmit={handleModalSubmit} className="hero-modal-form">
                {/* Image Upload or URL Input */}
                <div className="hero-modal-field">
                  <label className="hero-field-label">
                    Slide Image (JPEG/PNG up to 5 MB)
                  </label>

                  <div className="hero-modal-image-preview">
                    {form.imageUrl ? (
                      <div className="hero-preview-frame">
                        <img src={assetUrl(form.imageUrl) ?? form.imageUrl} alt="Slide preview" />
                        <button
                          type="button"
                          className="hero-preview-remove"
                          onClick={() => setForm((p) => ({ ...p, imageUrl: '' }))}
                        >
                          <X size={14} /> Remove
                        </button>
                      </div>
                    ) : (
                      <div
                        className="hero-upload-dropzone"
                        onClick={() => modalFileInputRef.current?.click()}
                      >
                        <UploadCloud size={32} />
                        <span>Click to choose or drag & drop photo</span>
                        <small>Landscape photos work best for hero slideshow</small>
                      </div>
                    )}

                    <input
                      ref={modalFileInputRef}
                      type="file"
                      accept="image/jpeg,image/png"
                      onChange={handleModalImageUpload}
                      style={{ display: 'none' }}
                      disabled={modalUploading}
                    />

                    {modalUploading && (
                      <p className="hero-uploading-indicator">
                        <RefreshCw size={14} className="animate-spin" /> Uploading image…
                      </p>
                    )}
                  </div>

                  <div style={{ marginTop: '10px' }}>
                    <small style={{ color: '#68756a', display: 'block', marginBottom: '4px' }}>
                      Or paste an image URL directly:
                    </small>
                    <input
                      type="text"
                      className="hero-input"
                      placeholder="https://images.unsplash.com/... or /uploads/..."
                      value={form.imageUrl}
                      onChange={(e) => setForm((p) => ({ ...p, imageUrl: e.target.value }))}
                      required
                    />
                  </div>
                </div>

                <div className="hero-modal-row">
                  <div className="hero-modal-field">
                    <label className="hero-field-label">Kannada Caption (shown on slide)</label>
                    <input
                      type="text"
                      className="hero-input"
                      placeholder="ಉದಾ: ಹಸಿರು ಛತ್ರ"
                      value={form.titleKn || ''}
                      onChange={(e) => setForm((p) => ({ ...p, titleKn: e.target.value }))}
                    />
                  </div>

                  <div className="hero-modal-field">
                    <label className="hero-field-label">English Caption (shown on slide)</label>
                    <input
                      type="text"
                      className="hero-input"
                      placeholder="e.g. The green canopy"
                      value={form.titleEn || ''}
                      onChange={(e) => setForm((p) => ({ ...p, titleEn: e.target.value }))}
                    />
                  </div>
                </div>

                <div className="hero-modal-field">
                  <label className="hero-field-label">Alt text (for accessibility)</label>
                  <input
                    type="text"
                    className="hero-input"
                    placeholder="Short description of the photo"
                    value={form.altText || ''}
                    onChange={(e) => setForm((p) => ({ ...p, altText: e.target.value }))}
                  />
                </div>

                <div className="hero-modal-checkbox">
                  <label>
                    <input
                      type="checkbox"
                      checked={form.active}
                      onChange={(e) => setForm((p) => ({ ...p, active: e.target.checked }))}
                    />
                    <span>Active (visible in continuous hero slideshow)</span>
                  </label>
                </div>

                <div className="hero-modal-actions">
                  <button
                    type="button"
                    className="plant-management-secondary"
                    onClick={() => setModalOpen(false)}
                    disabled={saving}
                  >
                    Cancel
                  </button>

                  <button
                    type="submit"
                    className="plant-management-primary"
                    disabled={saving || modalUploading || !form.imageUrl}
                  >
                    <Save size={16} />
                    {saving ? 'Saving…' : 'Save slide'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
