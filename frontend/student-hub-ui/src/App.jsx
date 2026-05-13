import './styles.css';

const sections = [
  {
    title: 'Заметки',
    text: 'Здесь будет список учебных заметок студента.',
  },
  {
    title: 'Дедлайны',
    text: 'Здесь будет список ближайших учебных дедлайнов.',
  },
];

export default function App() {
  return (
    <main className="app-shell">
      <section className="intro">
        <p className="eyebrow">Student Hub</p>
        <h1>Студенческий хаб</h1>
        <p className="lead">
          Базовый каркас приложения для учебных заметок и дедлайнов.
        </p>
      </section>

      <section className="workspace" aria-label="Разделы приложения">
        {sections.map((section) => (
          <article className="section-card" key={section.title}>
            <div className="card-marker" aria-hidden="true" />
            <h2>{section.title}</h2>
            <p>{section.text}</p>
          </article>
        ))}
      </section>
    </main>
  );
}
