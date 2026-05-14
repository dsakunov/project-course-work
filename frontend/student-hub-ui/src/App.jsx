import { useEffect, useState } from 'react';
import './styles.css';

const NOTES_API = 'http://localhost:8081/api/notes';
const DEADLINES_API = 'http://localhost:8082/api/deadlines';

const emptyNote = {
  title: '',
  content: '',
};

const emptyDeadline = {
  title: '',
  dueDate: new Date().toISOString().slice(0, 10),
};

export default function App() {
  const [notes, setNotes] = useState([]);
  const [deadlines, setDeadlines] = useState([]);
  const [noteForm, setNoteForm] = useState(emptyNote);
  const [deadlineForm, setDeadlineForm] = useState(emptyDeadline);
  const [editingNoteId, setEditingNoteId] = useState(null);
  const [editingDeadlineId, setEditingDeadlineId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setIsLoading(true);
    setError('');

    try {
      const [notesResponse, deadlinesResponse] = await Promise.all([
        fetch(NOTES_API),
        fetch(DEADLINES_API),
      ]);

      if (!notesResponse.ok || !deadlinesResponse.ok) {
        throw new Error('Не удалось загрузить данные');
      }

      setNotes(await notesResponse.json());
      setDeadlines(await deadlinesResponse.json());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsLoading(false);
    }
  }

  async function saveNote(event) {
    event.preventDefault();
    const url = editingNoteId ? `${NOTES_API}/${editingNoteId}` : NOTES_API;
    const method = editingNoteId ? 'PUT' : 'POST';

    await sendJson(url, method, noteForm);
    setNoteForm(emptyNote);
    setEditingNoteId(null);
    await loadData();
  }

  async function saveDeadline(event) {
    event.preventDefault();
    const url = editingDeadlineId
      ? `${DEADLINES_API}/${editingDeadlineId}`
      : DEADLINES_API;
    const method = editingDeadlineId ? 'PUT' : 'POST';

    await sendJson(url, method, deadlineForm);
    setDeadlineForm(emptyDeadline);
    setEditingDeadlineId(null);
    await loadData();
  }

  async function sendJson(url, method, body) {
    setError('');
    const response = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      setError('Не удалось сохранить данные');
    }
  }

  async function deleteNote(id) {
    await fetch(`${NOTES_API}/${id}`, { method: 'DELETE' });
    await loadData();
  }

  async function deleteDeadline(id) {
    await fetch(`${DEADLINES_API}/${id}`, { method: 'DELETE' });
    await loadData();
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
        <button className="secondary-button" type="button" onClick={loadData}>
          Обновить
        </button>
      </header>

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
            ) : deadlines.length === 0 ? (
              <p className="muted">Дедлайнов пока нет.</p>
            ) : (
              deadlines.map((deadline) => (
                <article className="list-item" key={deadline.id}>
                  <div>
                    <h3>{deadline.title}</h3>
                    <p>{formatDate(deadline.dueDate)}</p>
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
                  </div>
                </article>
              ))
            )}
          </div>
        </article>
      </section>
    </main>
  );
}

function formatDate(value) {
  if (!value) {
    return 'Дата не указана';
  }

  return new Intl.DateTimeFormat('ru-RU').format(new Date(value));
}
