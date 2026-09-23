import type { CreateItemRequest, Item } from '../types/item'
import { api } from './client'

export const itemsApi = {
  list: () => api.get<Item[]>('/items'),
  create: (data: CreateItemRequest) => api.post<Item>('/items', data),
  remove: (id: number) => api.delete(`/items/${id}`),
}
