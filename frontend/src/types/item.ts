// Должно совпадать с ItemDto на бэкенде
export interface Item {
  id: number
  title: string
  description: string | null
  createdAt: string
}

export interface CreateItemRequest {
  title: string
  description?: string
}
