const BASE_URL = '/api'

async function request(url, options = {}) {
  const res = await fetch(BASE_URL + url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    throw new Error(`请求失败(${res.status})`)
  }
  return res.json()
}

/**
 * 根据后端基础路径创建一套增删改查接口
 * @param {string} base 例如 '/tags'
 */
export function createCrudApi(base) {
  return {
    list: () => request(`${base}/list`),
    save: (data) => request(base, { method: 'POST', body: JSON.stringify(data) }),
    update: (data) => request(base, { method: 'PUT', body: JSON.stringify(data) }),
    getById: (id) => request(`${base}/${id}`),
    remove: (id) => request(`${base}/${id}`, { method: 'DELETE' }),
  }
}
