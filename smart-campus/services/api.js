import axios from 'axios';

const token = '';

export const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: token ? { Authorization: `Bearer ${token}` } : {}
});
