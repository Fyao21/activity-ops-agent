import request from './request'

export function queryAgent(data) {
  return request({
    url: '/agent/query',
    method: 'post',
    data
  })
}
