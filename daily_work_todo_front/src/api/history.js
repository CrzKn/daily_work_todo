import { request } from './crud'

const base = '/personalTask'

export const historyApi = {
  list: () => request(`${base}/history`),
}
