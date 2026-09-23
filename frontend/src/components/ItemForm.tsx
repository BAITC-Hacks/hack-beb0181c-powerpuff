import { useState, type FormEvent } from 'react'
import type { CreateItemRequest } from '../types/item'

interface Props {
  onSubmit: (data: CreateItemRequest) => Promise<void>
}

export function ItemForm({ onSubmit }: Props) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!title.trim()) return
    await onSubmit({ title, description })
    setTitle('')
    setDescription('')
  }

  return (
    <form className="item-form" onSubmit={handleSubmit}>
      <input
        placeholder="Название"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
      />
      <input
        placeholder="Описание"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <button type="submit">Добавить</button>
    </form>
  )
}
