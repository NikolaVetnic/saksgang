import { createBrowserRouter, type RouteObject } from 'react-router'
import { HomePage } from './HomePage.tsx'
import { Layout } from './Layout.tsx'
import { NotFoundPage } from './NotFoundPage.tsx'

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
