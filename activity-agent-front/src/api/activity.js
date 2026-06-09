import request from './request'

export function createActivity(data) {
  return request({
    url: '/activity/create',
    method: 'post',
    data
  })
}

export function getActivityList(params) {
  return request({
    url: '/activity/list',
    method: 'get',
    params
  })
}

export function getActivityDetail(id) {
  return request({
    url: `/activity/${id}`,
    method: 'get'
  })
}

export function updateActivity(data) {
  return request({
    url: '/activity/update',
    method: 'put',
    data
  })
}

export function participateActivity(data) {
  return request({
    url: '/activity/participate',
    method: 'post',
    data
  })
}

export function sendReward(data) {
  return request({
    url: '/reward/send',
    method: 'post',
    data
  })
}

export function getStatistics(params) {
  return request({
    url: '/statistics/activity',
    method: 'get',
    params
  })
}
