import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

// Features add their endpoints with api.injectEndpoints()
export const api = createApi({
  baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
  endpoints: () => ({}),
})
