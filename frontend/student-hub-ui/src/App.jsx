import { useEffect, useMemo, useState } from 'react';
import {
  BrowserRouter,
  Link,
  Navigate,
  Route,
  Routes,
  useNavigate,
} from 'react-router-dom';
import './styles.css';

const NOTES_SERVICE_URL =
    import.meta.env.VITE_NOTES_SERVICE_URL || 'http://localhost:8081';
const DEADLINES_SERVICE_URL =
    import.meta.env.VITE_DEADLINES_SERVICE_URL || 'http://localhost:8082';
const AUTH_API = `${NOTES_SERVICE_URL}/api/auth`;
const NOTES_API = `${NOTES_SERVICE_URL}/api/notes`;
const DEADLINES_API = `${DEADLINES_SERVICE_URL}/api/deadlines`;
const DASHBOARD_API = `${DEADLINES_SERVICE_URL}/api/dashboard`;
const CSRF_API = `${NOTES_SERVICE_URL}/api/csrf`;

const emptyNote = {
  title: '',
  content: '',
};

const emptyDeadline = {
  title: '',
  dueDate: new Date().toISOString().slice(0, 10),
  completed: false,
  important: false,
};

export default function App() {
  const [user, setUser] = useState(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);

  useEffect(() => {
    checkSession();
  }, []);

  async function checkSession() {
    try {
      const response = await fetch(`${AUTH_API}/session`, {
        credentials: 'include',
      });

      if (response.ok) {
        setUser(await response.json());
        await loadCsrfToken();
      }
    } finally {
      setIsCheckingSession(false);
    }
  }

  async function logout() {
    await fetch(`${AUTH_API}/logout`, {
      method: 'POST',
      credentials: 'include',
    }).catch(() => null);
    setUser(null);
  }

  if (isCheckingSession) {
    return <main className="auth-shell"><p className="muted">Загрузка...</p></main>;
  }

  return (
      <BrowserRouter>
        <Routes>
          <Route
              path="/login"
              element={
                user ? (
                    <Navigate to="/" replace />
                ) : (
                    <AuthPage mode="login" onLogin={setUser} />
                )
              }
          />
          <Route
              path="/register"
              element={
                user ? (
                    <Navigate to="/" replace />
                ) : (
                    <AuthPage mode="register" onLogin={setUser} />
                )
              }
          />
          <Route
              path="/"
              element={
                user ? (
                    <HubPage onLogout={logout} />
                ) : (
                    <Navigate to="/login" replace />
                )
              }
          />
        </Routes>
      </BrowserRouter>
  );
}

function AuthPage({ mode, onLogin }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const isRegister = mode === 'register';

  async function submit(event) {
    event.preventDefault();
    setError('');

    try {
      const response = await fetch(`${AUTH_API}/${isRegister ? 'register' : 'login'}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(form),
        credentials: 'include',
      });

      if (!response.ok) {
        const body = await response.json().catch(() => null);
        throw new Error(body?.message || 'Ошибка авторизации');
      }

      const body = await response.json();
      await loadCsrfToken();
      onLogin(body);
      navigate('/');
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return (
      <main className="auth-shell">
        <section className="auth-panel">
          <p className="eyebrow">Student Hub</p>
          <h1>{isRegister ? 'Регистрация' : 'Вход'}</h1>
          <p className="auth-copy">
            {isRegister
                ? 'Создайте учебную учётную запись.'
                : 'Войдите, чтобы открыть свои заметки и дедлайны.'}
          </p>

          {error && <p className="error-message">{error}</p>}

          <form className="entry-form" onSubmit={submit}>
            <label>
              Логин
              <input
                  required
                  minLength="3"
                  value={form.username}
                  onChange={(event) =>
                      setForm({ ...form, username: event.target.value })
                  }
                  placeholder="student"
              />
            </label>
            <label>
              Пароль
              <input
                  required
                  minLength="4"
                  type="password"
                  value={form.password}
                  onChange={(event) =>
                      setForm({ ...form, password: event.target.value })
                  }
                  placeholder="password123"
              />
            </label>
            <button type="submit">{isRegister ? 'Зарегистрироваться' : 'Войти'}</button>
          </form>

          <p className="auth-switch">
            {isRegister ? 'Уже есть аккаунт?' : 'Нет аккаунта?'}{' '}
            <Link to={isRegister ? '/login' : '/register'}>
              {isRegister ? 'Войти' : 'Зарегистрироваться'}
            </Link>
          </p>
        </section>
      </main>
  );
}

function HubPage({ onLogout }) {
  const [notes, setNotes] = useState([]);
  const [deadlines, setDeadlines] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [noteForm, setNoteForm] = useState(emptyNote);
  const [deadlineForm, setDeadlineForm] = useState(emptyDeadline);
  const [editingNoteId, setEditingNoteId] = useState(null);
  const [editingDeadlineId, setEditingDeadlineId] = useState(null);
  const [openDeadlineMenuId, setOpenDeadlineMenuId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const sortedDeadlines = useMemo(
      () => [...deadlines].sort(compareDeadlines),
      [deadlines],
  );

  useEffect(() => {
    loadData();
  }, []);

  async function authFetch(url, options = {}) {
    const response = await fetch(url, {
      ...options,
      credentials: 'include',
      headers: {
        ...csrfHeader(options.method),
        ...options.headers,
      },
    });

    if (response.status === 401 || response.status === 403) {
      onLogout();
      throw new Error('Сессия истекла. Войдите снова.');
    }

    return response;
  }

  async function loadData() {
    setIsLoading(true);
    setError('');

    const [notesResult, deadlinesResult, dashboardResult] = await Promise.allSettled([
      authFetch(NOTES_API),
      authFetch(DEADLINES_API),
      authFetch(DASHBOARD_API),
    ]);

    const errors = [];

    if (notesResult.status === 'fulfilled' && notesResult.value.ok) {
      setNotes(await notesResult.value.json());
    } else {
      setNotes([]);
      errors.push('Заметки временно недоступны');
    }

    if (deadlinesResult.status === 'fulfilled' && deadlinesResult.value.ok) {
      setDeadlines(await deadlinesResult.value.json());
    } else {
      errors.push('Не удалось загрузить дедлайны');
    }

    if (dashboardResult.status === 'fulfilled' && dashboardResult.value.ok) {
      setDashboard(await dashboardResult.value.json());
    } else {
      errors.push('Не удалось загрузить сводку');
    }

    if (errors.length > 0) {
      const authError = [
        notesResult,
        deadlinesResult,
        dashboardResult,
      ].find((result) => result.status === 'rejected');

      setError(authError?.reason?.message || errors.join('. '));
    }

    setIsLoading(false);
  }

  async function saveNote(event) {
    event.preventDefault();
    const url = editingNoteId ? `${NOTES_API}/${editingNoteId}` : NOTES_API;
    const method = editingNoteId ? 'PUT' : 'POST';

    if (await sendJson(url, method, noteForm)) {
      setNoteForm(emptyNote);
      setEditingNoteId(null);
      await loadData();
    }
  }

  async function saveDeadline(event) {
    event.preventDefault();
    const url = editingDeadlineId
        ? `${DEADLINES_API}/${editingDeadlineId}`
        : DEADLINES_API;
    const method = editingDeadlineId ? 'PUT' : 'POST';

    if (await sendJson(url, method, deadlineForm)) {
      setDeadlineForm(emptyDeadline);
      setEditingDeadlineId(null);
      setOpenDeadlineMenuId(null);
      await loadData();
    }
  }

  async function sendJson(url, method, body) {
    setError('');
    const response = await authFetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      setError('Не удалось сохранить данные');
      return false;
    }

    return true;
  }

  async function deleteNote(id) {
    await authFetch(`${NOTES_API}/${id}`, { method: 'DELETE' });
    await loadData();
  }

  async function deleteDeadline(id) {
    await authFetch(`${DEADLINES_API}/${id}`, { method: 'DELETE' });
    setOpenDeadlineMenuId(null);
    await loadData();
  }

  async function updateDeadlineState(deadline, patch) {
    const nextDeadline = {
      title: deadline.title,
      dueDate: deadline.dueDate,
      completed: deadline.completed,
      important: deadline.important,
      ...patch,
    };

    if (await sendJson(`${DEADLINES_API}/${deadline.id}`, 'PUT', nextDeadline)) {
      setOpenDeadlineMenuId(null);
      await loadData();
    }
  }

  function startNoteEdit(note) {
    setEditingNoteId(note.id);
    setNoteForm({
      title: note.title,
      content: note.content,
    });
  }

  function startDeadlineEdit(deadline) {
    setEditingDeadlineId(deadline.id);
    setDeadlineForm({
      title: deadline.title,
      dueDate: deadline.dueDate,
      completed: deadline.completed,
      important: deadline.important,
    });
  }

  function cancelNoteEdit() {
    setEditingNoteId(null);
    setNoteForm(emptyNote);
  }

  function cancelDeadlineEdit() {
    setEditingDeadlineId(null);
    setDeadlineForm(emptyDeadline);
  }

  return (
      <main className="app-shell">
        <header className="topbar">
          <div>
            <p className="eyebrow">Student Hub</p>
            <h1>Студенческий хаб</h1>
          </div>
          <div className="topbar-actions">
            <button className="secondary-button" type="button" onClick={loadData}>
              Обновить
            </button>
            <button className="danger-button" type="button" onClick={onLogout}>
              Выйти
            </button>
          </div>
        </header>

        {dashboard && (
            <section className="summary-row" aria-label="Сводка">
              <SummaryItem label="Заметки" value={dashboard.notesCount} />
              <SummaryItem label="Дедлайны" value={dashboard.deadlinesCount} />
              <SummaryItem
                  label="Связь сервисов"
                  value={dashboard.notesServiceAvailable ? 'OK' : 'Нет'}
              />
            </section>
        )}

        {error && <p className="error-message">{error}</p>}

        <section className="workspace" aria-label="Учебные данные">
          <article className="panel">
            <div className="panel-heading">
              <div>
                <div className="card-marker" aria-hidden="true" />
                <h2>Заметки</h2>
              </div>
              <span className="counter">{notes.length}</span>
            </div>

            <form className="entry-form" onSubmit={saveNote}>
              <label>
                Название
                <input
                    required
                    value={noteForm.title}
                    onChange={(event) =>
                        setNoteForm({ ...noteForm, title: event.target.value })
                    }
                    placeholder="Лабораторная работа"
                />
              </label>
              <label>
                Текст
                <textarea
                    required
                    rows="4"
                    value={noteForm.content}
                    onChange={(event) =>
                        setNoteForm({ ...noteForm, content: event.target.value })
                    }
                    placeholder="Сдать отчёт по Java"
                />
              </label>
              <div className="form-actions">
                <button type="submit">
                  {editingNoteId ? 'Сохранить' : 'Добавить'}
                </button>
                {editingNoteId && (
                    <button
                        className="secondary-button"
                        type="button"
                        onClick={cancelNoteEdit}
                    >
                      Отмена
                    </button>
                )}
              </div>
            </form>

            <div className="list">
              {isLoading ? (
                  <p className="muted">Загрузка...</p>
              ) : notes.length === 0 ? (
                  <p className="muted">Заметок пока нет.</p>
              ) : (
                  notes.map((note) => (
                      <article className="list-item" key={note.id}>
                        <div>
                          <h3>{note.title}</h3>
                          <p>{note.content}</p>
                        </div>
                        <div className="item-actions">
                          <button
                              className="secondary-button"
                              type="button"
                              onClick={() => startNoteEdit(note)}
                          >
                            Изменить
                          </button>
                          <button
                              className="danger-button"
                              type="button"
                              onClick={() => deleteNote(note.id)}
                          >
                            Удалить
                          </button>
                        </div>
                      </article>
                  ))
              )}
            </div>
          </article>

          <article className="panel">
            <div className="panel-heading">
              <div>
                <div className="card-marker deadline-marker" aria-hidden="true" />
                <h2>Дедлайны</h2>
              </div>
              <span className="counter">{deadlines.length}</span>
            </div>

            <form className="entry-form" onSubmit={saveDeadline}>
              <label>
                Название
                <input
                    required
                    value={deadlineForm.title}
                    onChange={(event) =>
                        setDeadlineForm({
                          ...deadlineForm,
                          title: event.target.value,
                        })
                    }
                    placeholder="Защита проекта"
                />
              </label>
              <label>
                Дата
                <input
                    required
                    type="date"
                    value={deadlineForm.dueDate}
                    onChange={(event) =>
                        setDeadlineForm({
                          ...deadlineForm,
                          dueDate: event.target.value,
                        })
                    }
                />
              </label>
              <div className="form-actions">
                <button type="submit">
                  {editingDeadlineId ? 'Сохранить' : 'Добавить'}
                </button>
                {editingDeadlineId && (
                    <button
                        className="secondary-button"
                        type="button"
                        onClick={cancelDeadlineEdit}
                    >
                      Отмена
                    </button>
                )}
              </div>
            </form>

            <div className="list">
              {isLoading ? (
                  <p className="muted">Загрузка...</p>
              ) : sortedDeadlines.length === 0 ? (
                  <p className="muted">Дедлайнов пока нет.</p>
              ) : (
                  sortedDeadlines.map((deadline) => {
                    const countdown = getDeadlineCountdown(deadline.dueDate);

                    return (
                        <article
                            className={`list-item deadline-card ${getDeadlineCardClass(deadline)}`}
                            key={deadline.id}
                        >
                          <div className="deadline-menu">
                            <button
                                className="icon-button"
                                type="button"
                                aria-label="Меню дедлайна"
                                onClick={() =>
                                    setOpenDeadlineMenuId(
                                        openDeadlineMenuId === deadline.id ? null : deadline.id,
                                    )
                                }
                            >
                              ...
                            </button>
                            {openDeadlineMenuId === deadline.id && (
                                <div className="deadline-menu-list">
                                  <button
                                      type="button"
                                      onClick={() =>
                                          updateDeadlineState(deadline, {
                                            important: !deadline.important,
                                          })
                                      }
                                  >
                                    {deadline.important
                                        ? 'Убрать важное'
                                        : 'Отметить важным'}
                                  </button>
                                </div>
                            )}
                          </div>

                          <div className="deadline-content">
                            <label className="deadline-check">
                              <input
                                  type="checkbox"
                                  checked={deadline.completed}
                                  onChange={(event) =>
                                      updateDeadlineState(deadline, {
                                        completed: event.target.checked,
                                      })
                                  }
                              />
                              <span className="deadline-text">
                        <span className="deadline-title-row">
                          <span>{deadline.title}</span>
                          {deadline.important && (
                              <strong className="important-badge">
                                <span aria-hidden="true">!</span>
                                Важное
                              </strong>
                          )}
                        </span>
                        <span className="deadline-date">
                          {formatDate(deadline.dueDate)}
                        </span>
                      </span>
                            </label>
                          </div>

                          <div className="item-actions">
                            <button
                                className="secondary-button"
                                type="button"
                                onClick={() => startDeadlineEdit(deadline)}
                            >
                              Изменить
                            </button>
                            <button
                                className="danger-button"
                                type="button"
                                onClick={() => deleteDeadline(deadline.id)}
                            >
                              Удалить
                            </button>
                            <span className={`deadline-countdown ${countdown.isOverdue ? 'overdue' : ''}`}>
                      {countdown.text}
                    </span>
                          </div>
                        </article>
                    );
                  })
              )}
            </div>
          </article>
        </section>
      </main>
  );
}

async function loadCsrfToken() {
  await fetch(CSRF_API, {
    credentials: 'include',
  }).catch(() => null);
}

function csrfHeader(method) {
  if (!method || method === 'GET') {
    return {};
  }

  const token = document.cookie
      .split('; ')
      .find((row) => row.startsWith('XSRF-TOKEN='))
      ?.split('=')[1];

  return token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {};
}

function SummaryItem({ label, value }) {
  return (
      <article className="summary-item">
        <span>{label}</span>
        <strong>{value ?? 'Нет данных'}</strong>
      </article>
  );
}

function compareDeadlines(first, second) {
  const firstTime = parseLocalDate(first.dueDate).getTime();
  const secondTime = parseLocalDate(second.dueDate).getTime();

  if (firstTime !== secondTime) {
    return firstTime - secondTime;
  }

  return first.id - second.id;
}

function getDeadlineCardClass(deadline) {
  if (deadline.completed) {
    return 'deadline-card-completed';
  }

  const daysLeft = getDaysLeft(deadline.dueDate);
  if (daysLeft >= 0 && daysLeft <= 2) {
    return 'deadline-card-urgent';
  }

  return '';
}

function getDeadlineCountdown(value) {
  const daysLeft = getDaysLeft(value);

  if (daysLeft < 0) {
    return {
      text: `Просрочено на ${formatDayWord(Math.abs(daysLeft))}`,
      isOverdue: true,
    };
  }

  if (daysLeft === 0) {
    return {
      text: 'Осталось: сегодня',
      isOverdue: false,
    };
  }

  return {
    text: `Осталось: ${formatDayWord(daysLeft)}`,
    isOverdue: false,
  };
}

function getDaysLeft(value) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const dueDate = parseLocalDate(value);
  dueDate.setHours(0, 0, 0, 0);

  return Math.round((dueDate.getTime() - today.getTime()) / 86400000);
}

function parseLocalDate(value) {
  if (!value) {
    return new Date();
  }

  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function formatDayWord(days) {
  const mod10 = days % 10;
  const mod100 = days % 100;

  if (mod10 === 1 && mod100 !== 11) {
    return `${days} день`;
  }

  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
    return `${days} дня`;
  }

  return `${days} дней`;
}

function formatDate(value) {
  if (!value) {
    return 'Дата не указана';
  }

  return new Intl.DateTimeFormat('ru-RU').format(parseLocalDate(value));
}