import { useEffect, useState } from 'react'
import { itemsApi } from '../api/items'
import { ItemForm } from '../components/ItemForm'
import type { CreateItemRequest, Item } from '../types/item'

export function HomePage() {
  const [items, setItems] = useState<Item[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    itemsApi
      .list()
      .then(setItems)
      .catch(() => setError('Бэкенд недоступен. Запущен ли Spring Boot на :8080?'))
  }, [])

  async function handleCreate(data: CreateItemRequest) {
    const created = await itemsApi.create(data)
    setItems((prev) => [...prev, created])
  }

  async function handleDelete(id: number) {
    await itemsApi.remove(id)
    setItems((prev) => prev.filter((i) => i.id !== id))
  }

  return (
    <main className="container">
      <h1>Powerpuff</h1>
      <p>Шаблон Spring Boot + React. Замените Item на свою сущность.</p>

      {error && <p className="error">{error}</p>}

      <ItemForm onSubmit={handleCreate} />

      <ul className="item-list">
        {items.map((item) => (
          <li key={item.id}>
            <div>
              <strong>{item.title}</strong>
              {item.description && <span> — {item.description}</span>}
            </div>
            <button onClick={() => handleDelete(item.id)}>✕</button>
          </li>
        ))}
      </ul>
    </main>
  )
}
