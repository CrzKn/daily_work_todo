import { createCrudApi, request } from './crud'

const base = '/periodicTask'

export const periodicApi = {
  ...createCrudApi(base),
  generate: () => request(`${base}/generate`, { method: 'POST' }),
  setEnable: (id, enable) => request(`${base}/${id}/enable/${enable}`, { method: 'PUT' }),
  retry: (id) => request(`${base}/${id}/retry`, { method: 'POST' }),
}
